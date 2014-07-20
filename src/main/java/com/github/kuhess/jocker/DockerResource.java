package com.github.kuhess.jocker;

import com.github.dockerjava.client.DockerClient;
import com.github.dockerjava.client.NotFoundException;
import com.github.dockerjava.client.command.CreateContainerCmd;
import com.github.dockerjava.client.model.*;
import com.sun.jersey.api.client.ClientResponse;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.util.*;

public class DockerResource extends ExternalResource {

    private final String image;
    private final ResourceChecker checker;

    private final DockerClient dockerClient;
    private final String prefix = "jocker";

    private String[] env = new String[0];

    private String host;
    private Map<Integer, Integer> ports;

    public DockerResource(String image) {
        this(image, ResourceChecker.alwaysTrue());
    }

    public DockerResource(String image, ResourceChecker checker) {
        this.image = image;
        this.checker = checker;

        dockerClient = new DockerClient("http://localhost:2375");
        try {
            dockerClient.inspectImageCmd(this.image).exec();
        } catch (NotFoundException e) {
            ClientResponse resp = dockerClient.pullImageCmd(this.image).exec();
            // Wait for stream
            String message;
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
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image)
                .withName(prefix + "-" + UUID.randomUUID());

        if (env.length > 0) {
            containerCmd.withEnv(env);
        }

        ContainerCreateResponse container = containerCmd.exec();

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

        Map<ExposedPort, Ports.Binding> bindings = inspection.getNetworkSettings().getPorts().getBindings();
        ports = new HashMap<>();
        for (Map.Entry<ExposedPort, Ports.Binding> binding : bindings.entrySet()) {
            ports.put(binding.getKey().getPort(), binding.getValue().getHostPort());
        }

        this.host = "0.0.0.0";

        this.checker.waitForAvailability(this.host, this.ports);
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

    public int getPort(int port) {
        return this.ports.get(port);
    }

    public String getHost() {
        return this.host;
    }

    public DockerResource withEnv(String... env) {
        this.env = env;
        return this;
    }
}
