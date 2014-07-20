package com.github.kuhess.jocker;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ElasticsearchDemo {

    @Rule
    public DockerResource elasticsearchResource = new DockerResource(
            "barnybug/elasticsearch:1.2.1",
            new ResourceChecker() {
                @Override
                protected boolean isAvailable(String host, Map<Integer, Integer> ports) throws Exception {
                    Client client = new TransportClient().addTransportAddress(
                            new InetSocketTransportAddress(
                                    host,
                                    ports.get(9300)
                            )
                    );

                    ClusterHealthResponse response = client.admin().cluster()
                            .prepareHealth()
                            .execute()
                            .actionGet();

                    return ClusterHealthStatus.RED != response.getStatus();
                }
            }
    );

    @Test
    public void demo_elasticsearch() throws IOException, InterruptedException {
        Client client = new TransportClient().addTransportAddress(
                new InetSocketTransportAddress(
                        elasticsearchResource.getHost(),
                        elasticsearchResource.getPort(9300)
                )
        );

        ClusterStateResponse response = client.admin().cluster().prepareState().execute().actionGet();

        assertEquals("elasticsearch", response.getClusterName().value());
    }
}