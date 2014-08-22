package com.github.kuhess.jocker;

import com.github.dockerjava.client.DockerClient;
import com.github.dockerjava.client.model.Container;
import com.github.dockerjava.client.model.Image;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class JockerResourceITest {

    private DockerClient client = new DockerClient();

    @BeforeClass
    public static void cleanDockerContainers() throws IOException {
        // /!\ Kill and remove all containers and images!! Run it in a VM!!!
        DockerClient dockerClient = new DockerClient();

        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        for (Container container : containers) {
            dockerClient.killContainerCmd(container.getId()).exec();
            dockerClient.removeContainerCmd(container.getId()).exec();
        }

        List<Image> images = dockerClient.listImagesCmd().withShowAll(true).exec();
        for (Image image : images) {
            dockerClient.removeImageCmd(image.getId()).exec();
        }
    }

    @Test
    public void usage() throws Exception {
        JockerResource resource = JockerResource.fromFolder(
                client,
                new File(Resources.getResource("docker").toURI()),
                new ResourceChecker() {
                    @Override
                    protected boolean isAvailable(String host, Map<Integer, Integer> ports) throws Exception {
                        Thread.sleep(1000);
                        return true;
                    }
                }
        );

        try {
            resource.startAndWait();

            String url = "http://" + resource.getHost() + ":" + resource.getPortOf(8080);
            URLConnection connection = new URL(url).openConnection();
            InputStream response = connection.getInputStream();
            assertEquals("pong\n", CharStreams.toString(new InputStreamReader(response)));
        } finally {
            resource.stopAndWait();
        }

    }
}