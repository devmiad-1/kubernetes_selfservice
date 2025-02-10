package com.miad.tech.image;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DockerImageUploader {
    private final DockerClient dockerClient;
    private final String username;
    private final String password;
    private final String registry;

    public DockerImageUploader(String username, String password, String registry) {
        this.dockerClient = DockerClientBuilder.getInstance().build();
        this.username = username;
        this.password = password;
        this.registry = registry;
    }

    public String buildImage(String dockerfilePath, String imageName) throws IOException {
        System.out.println("Building image: " + imageName);

        Path path = Paths.get(dockerfilePath);
        File dockerFile = path.toFile();

        BuildImageCmd buildImageCmd = dockerClient.buildImageCmd(dockerFile)
                .withTags(java.util.Collections.singleton(imageName));

        String imageId = buildImageCmd.exec(new BuildImageResultCallback())
                .awaitImageId();

        System.out.println("Image built successfully with ID: " + imageId);
        return imageId;
    }

    public void pushImage(String imageName) {
        System.out.println("Pushing image: " + imageName);

        // Create auth config for registry
        AuthConfig authConfig = new AuthConfig()
                .withUsername(username)
                .withPassword(password)
                .withRegistryAddress(registry);

        // Push the image
        try {
            PushImageCmd pushImageCmd = dockerClient.pushImageCmd(imageName)
                    .withAuthConfig(authConfig);

            pushImageCmd.exec(new PushImageResultCallback()).awaitSuccess();
            System.out.println("Image pushed successfully: " + imageName);
        } catch (Exception e) {
            System.err.println("Error pushing image: " + e.getMessage());
            throw new RuntimeException("Failed to push image", e);
        }
    }

    public void uploadImage(String dockerfilePath, String repository, String tag) {
        try {
            // Full image name with repository and tag
            String fullImageName = String.format("%s/%s:%s", registry, repository, tag);

            // Build the image
            buildImage(dockerfilePath, fullImageName);

            // Push to registry
            pushImage(fullImageName);

        } catch (Exception e) {
            System.err.println("Error uploading image: " + e.getMessage());
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    public static void main(String[] args) {
        if (args.length != 5) {
            System.out.println("Usage: java DockerImageUploader <dockerfile-path> <repository> <tag> <username> <password>");
            System.exit(1);
        }

        String dockerfilePath = args[0];
        String repository = args[1];
        String tag = args[2];
        String username = args[3];
        String password = args[4];

        // Default to Docker Hub
        String registry = "registry.hub.docker.com";

        DockerImageUploader uploader = new DockerImageUploader(username, password, registry);
        uploader.uploadImage(dockerfilePath, repository, tag);
    }
}