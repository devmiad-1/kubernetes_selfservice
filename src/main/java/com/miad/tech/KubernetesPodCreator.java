package com.miad.tech;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class KubernetesPodCreator {

    private final CoreV1Api api;
    private final String namespace;

    public KubernetesPodCreator(String namespace) throws IOException {
        // Load the default Kubernetes configuration
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        this.api = new CoreV1Api();
        this.namespace = namespace;
    }

    public V1Pod createPod(String podName, String imageName, String... args) throws ApiException {
        // Create the container
        V1Container container = new V1Container()
                .name(podName)
                .image(imageName)
                .args(args != null ? Arrays.asList(args) : null)
                .imagePullPolicy("IfNotPresent");

        // Create the pod spec
        V1PodSpec podSpec = new V1PodSpec()
                .containers(Collections.singletonList(container))
                .restartPolicy("Never");

        // Create the pod metadata
        V1ObjectMeta metadata = new V1ObjectMeta()
                .name(podName)
                .labels(Collections.singletonMap("app", podName));

        // Create the pod
        V1Pod pod = new V1Pod()
                .metadata(metadata)
                .spec(podSpec);

        // Create the pod in Kubernetes - fixed method signature
        return api.createNamespacedPod(
                namespace,    // namespace
                pod,         // pod body
                null,        // pretty
                null,        // dryRun
                null,        // fieldManager
                null         // fieldValidation
        );
    }

    public V1Pod getPodStatus(String podName) throws ApiException {
        return api.readNamespacedPod(podName, namespace, null);
    }

    public void deletePod(String podName) throws ApiException {
        V1DeleteOptions deleteOptions = new V1DeleteOptions();
        api.deleteNamespacedPod(
                podName,         // name
                namespace,       // namespace
                null,           // pretty
                null,           // dryRun
                null,           // gracePeriodSeconds
                null,           // orphanDependents
                null,           // propagationPolicy
                deleteOptions   // body
        );
    }

    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.out.println("Usage: java KubernetesPodCreator <pod-name> <image-name> [args...]");
                System.exit(1);
            }

            String podName = args[0];
            String imageName = args[1];
            String[] containerArgs = args.length > 2 ?
                    Arrays.copyOfRange(args, 2, args.length) : null;

            // Create pod in default namespace
            KubernetesPodCreator creator = new KubernetesPodCreator("default");

            // Create the pod
            System.out.println("Creating pod " + podName + " with image " + imageName);
            V1Pod pod = creator.createPod(podName, imageName, containerArgs);
            System.out.println("Pod created successfully");

            // Monitor pod status
            boolean running = true;
            while (running) {
                V1Pod status = creator.getPodStatus(podName);
                String phase = status.getStatus().getPhase();
                System.out.println("Pod status: " + phase);

                if (phase.equals("Succeeded") || phase.equals("Failed")) {
                    running = false;
                }

                Thread.sleep(1000);
            }

            // Get final status
            V1Pod finalStatus = creator.getPodStatus(podName);
            System.out.println("Pod finished with status: " +
                    finalStatus.getStatus().getPhase());

            // Cleanup
            System.out.println("Deleting pod...");
            creator.deletePod(podName);
            System.out.println("Pod deleted");

        } catch (IOException e) {
            System.err.println("Error loading Kubernetes config: " + e.getMessage());
        } catch (ApiException e) {
            System.err.println("Kubernetes API error: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting: " + e.getMessage());
        }
    }
}