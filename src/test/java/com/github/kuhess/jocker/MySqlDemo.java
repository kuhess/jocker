package com.github.kuhess.jocker;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.junit.Rule;
import org.junit.Test;

import java.sql.Connection;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class MySqlDemo {

    @Rule
    public DockerResource mySqlResource = new DockerResource(
            "mysql:5.6",
            new ResourceChecker() {
                @Override
                protected boolean isAvailable(String host, Map<Integer, Integer> ports) throws Exception {
                    MysqlDataSource dataSource = new MysqlDataSource();
                    dataSource.setServerName(host);
                    dataSource.setPortNumber(ports.get(3306));
                    dataSource.setUser("root");
                    dataSource.setPassword("plop");

                    Connection connection = dataSource.getConnection();

                    return connection.isValid(1);
                }
            }
    )
            .withEnv("MYSQL_ROOT_PASSWORD=plop");

    @Test
    public void demo_mysql() throws Exception {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setServerName(mySqlResource.getHost());
        dataSource.setPortNumber(mySqlResource.getPort(3306));
        dataSource.setUser("root");
        dataSource.setPassword("plop");

        Connection connection = dataSource.getConnection();

        assertTrue(connection.isValid(10));
    }
}