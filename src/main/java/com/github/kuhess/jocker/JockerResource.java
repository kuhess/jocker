package com.github.kuhess.jocker;

import com.github.dockerjava.client.DockerClient;
import com.github.dockerjava.client.NotFoundException;
import com.github.dockerjava.client.model.ContainerCreateResponse;
import com.github.dockerjava.client.model.ContainerInspectResponse;
import com.github.dockerjava.client.model.ExposedPort;
import com.github.dockerjava.client.model.Ports;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AbstractIdleService;
import com.sun.jersey.api.client.ClientResponse;

import java.io.File;
import java.util.Map;
import java.util.UUID;

public class JockerResource extends AbstractIdleService {

    private final DockerClient client;
    private final String imageName;
    private final ResourceChecker resourceChecker;

    private String containerId;
    private String host;
    private ImmutableMap<Integer, Integer> ports;

    private JockerResource(DockerClient client, String imageName, ResourceChecker resourceChecker) {
        this.client = client;
        this.imageName = imageName;
        this.resourceChecker = resourceChecker;
    }

    /**
     * Create a JockerResource from an image name.
     * <p/>
     * If the image is not available in the local cache, it is pulled from the Docker registry.
     *
     * @param client          Docker client
     * @param imageName       name of the image
     * @param resourceChecker the resource checker for the image
     * @return a JockerResource ready to be started
     */
    public static JockerResource fromImage(DockerClient client, String imageName, ResourceChecker resourceChecker) {
        return new JockerResource(client, imageName, resourceChecker);
    }

    /**
     * Create a JockerResource from a folder containing a Dockerfile.
     *
     * @param client          Docker client
     * @param dockerFolder    folder containing a Dockerfile
     * @param resourceChecker the resource checker for the image
     * @return a JockerResource ready to be started
     */
    public static JockerResource fromFolder(DockerClient client, File dockerFolder, ResourceChecker resourceChecker) {
        String tag = randomName();
        ClientResponse resp = client.buildImageCmd(dockerFolder)
                .withTag(tag)
                .exec();
        // Wait for stream
        String message = resp.getEntity(String.class);
        if (!message.contains("Successfully built")) {
            throw new RuntimeException("Fail to build folder " + dockerFolder.getAbsolutePath() + "\nOriginal message from Docker server:\n" + message);
        }

        return new JockerResource(client, tag, resourceChecker);
    }

    private static String randomName() {
        return "jocker-" + UUID.randomUUID().toString();
    }

    /**
     * Start a Docker container associated to this instance of JockerResource.
     * <p/>
     * This method must not be called directly.
     * Prefer to use {@link com.google.common.util.concurrent.Service#start()} or {@link com.google.common.util.concurrent.Service#startAndWait()}.
     */
    @Override
    protected void startUp() throws Exception {
        if (!imageIsPresent(this.imageName)) {
            pullImage(this.imageName);
        }

        // Create the container
        ContainerCreateResponse containerCreateResponse = this.client.createContainerCmd(this.imageName)
                .withName(randomName())
                .exec();

        this.containerId = containerCreateResponse.getId();

        // Start the container
        this.client.startContainerCmd(this.containerId)
                .withPublishAllPorts(true)
                .exec();

        // Get host and ports
        ContainerInspectResponse containerInspectResponse = this.client.inspectContainerCmd(this.containerId).exec();

        Map<ExposedPort, Ports.Binding> bindings = containerInspectResponse.getNetworkSettings().getPorts().getBindings();

        ImmutableMap.Builder<Integer, Integer> portsBuilder = ImmutableMap.builder();
        for (Map.Entry<ExposedPort, Ports.Binding> binding : bindings.entrySet()) {
            portsBuilder.put(binding.getKey().getPort(), binding.getValue().getHostPort());
        }
        this.ports = portsBuilder.build();
        this.host = "0.0.0.0"; // Wut?

        this.resourceChecker.waitForAvailability(this.host, this.ports);
    }

    /**
     * Stop the Docker container associated to this instance of JockerResource.
     * <p/>
     * This method must not be called directly.
     * Prefer to use {@link com.google.common.util.concurrent.Service#stop()} or {@link com.google.common.util.concurrent.Service#stopAndWait()}.
     */
    @Override
    protected void shutDown() throws Exception {
        this.client.stopContainerCmd(this.containerId).exec();
        this.client.removeContainerCmd(this.containerId).exec();
    }

    private void pullImage(String imageName) {
        ClientResponse resp = this.client.pullImageCmd(this.imageName).exec();
        // Wait for stream
        String message = resp.getEntity(String.class);
        if (!message.contains("Download complete")) {
            throw new RuntimeException("Fail to pull image " + this.imageName + "\nOriginal message from Docker server:\n" + message);
        }
    }

    private boolean imageIsPresent(String imageName) {
        try {
            this.client.inspectImageCmd(imageName).exec();
        } catch (NotFoundException e) {
            return false;
        }
        return true;
    }

    public String getHost() {
        return this.host;
    }

    public int getPortOf(int port) {
        return this.ports.get(port);
    }
}
