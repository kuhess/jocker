package com.github.kuhess.jocker;

import com.github.dockerjava.client.DockerClient;
import com.github.dockerjava.client.NotFoundException;
import com.github.dockerjava.client.model.*;
import com.sun.jersey.api.client.ClientResponse;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DockerResource extends ExternalResource {

    private final String image;
    private final int resourcePort;
    private final ResourceChecker checker;

    private final DockerClient dockerClient;
    private final String prefix = "jocker";

    private int port;
    private String host;

    public DockerResource(String image, int port, ResourceChecker checker) {
        this.image = image;
        this.resourcePort = port;
        this.checker = checker;

        dockerClient = new DockerClient("http://localhost:2375");
        try {
            dockerClient.inspectImageCmd(this.image).exec();
        } catch (NotFoundException e) {
            ClientResponse resp = dockerClient.pullImageCmd(this.image).exec();
            // Wait for stream
            String message = null;
            try {
                message = DockerClient.asString(resp);
            } catch (IOException e1) {
                throw new RuntimeException("blurp");
            }
            if (!message.contains("Download complete")) {
                throw new RuntimeException("fail to download");
            }
        }
    }

    @Override
    protected void before() throws Throwable {
        ContainerCreateResponse container = dockerClient.createContainerCmd(image)
                .withName(prefix + "-" + UUID.randomUUID())
                .withExposedPorts(ExposedPort.tcp(resourcePort))
                .exec();

        // Clean previous zombies
        // List<Container> containers = dockerClient.listContainersCmd()
        //         .withBefore(container.getId())
        //         .exec();
        // List<Container> zombies = filterOutMyContainers(containers);
        // for (Container zombie : zombies) {
        //     dockerClient.killContainerCmd(zombie.getId()).exec();
        //     dockerClient.removeContainerCmd(zombie.getId()).exec();
        // }

        // Start the container
        dockerClient.startContainerCmd(container.getId()).withPublishAllPorts(true).exec();

        // Retrieve its host and its port
        ContainerInspectResponse inspection = dockerClient.inspectContainerCmd(container.getId()).exec();
        Ports.Binding portBinding = inspection.getNetworkSettings().getPorts().getBindings().get(ExposedPort.tcp(resourcePort));
        this.port = portBinding.getHostPort();
        this.host = portBinding.getHostIp();

        this.checker.waitForAvailability();
    }

    @Override
    protected void after() {
        List<Container> containers = dockerClient.listContainersCmd().exec();
        List<Container> myContainers = filterOutMyContainers(containers);
        for (Container container : myContainers) {
            dockerClient.stopContainerCmd(container.getId()).exec();
            dockerClient.removeContainerCmd(container.getId()).exec();
        }
    }

    private List<Container> filterOutMyContainers(List<Container> containers) {
        List<Container> zombies = new ArrayList<>();
        for (Container container : containers) {
            String[] names = container.getNames();
            for (String name : names) {
                if (name.startsWith("/" + prefix)) {
                    zombies.add(container);
                }
            }
        }
        return zombies;
    }

    public int getPort() {
        return this.port;
    }

    public String getHost() {
        return this.host;
    }
}
