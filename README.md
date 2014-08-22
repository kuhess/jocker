# Test with Docker

First steps to use Docker as infrastructure provisionner for testing.

Check out the first [examples](src/test/java/com/github/kuhess/jocker).

## Usage

### Example with Redis

```java
// Create and start the resource
JockerResource resource = new JockerResource(
    "redis:2.8.13",
    new ResourceChecker() {
        @Override
        protected boolean isAvailable(String host, Map<Integer, Integer> ports) throws Exception {
            Jedis jedis = new Jedis(host, ports.get(6379));
            return "PONG".equals(jedis.ping());
        }
    }
);
resource.start();

// Use the container :)
String redisHost = resource.getHost();
int redisPort = resource.getPortOf(6379);
Jedis jedis = new Jedis(redisHost, redisPort);
/* ... */

// Stop the resource
resource.stop();
```

## Development

### With Vagrant

**Requirements**:

- [Vagrant](http://docs.vagrantup.com/v2/installation/index.html)

The Vagrant is configured with Java 7 (Oracle) and Docker 1.13.

Launch and connect to the Vagrant:

```sh
vagrant up
vagrant ssh
```

Run the tests:

```sh
cd /vagrant
./gradlew test
```

### From scratch

**Requirements**:

* Java 7
* Docker 1.13

The Docker server must use TCP. To set it up, add the following line to `/etc/default/docker`: `DOCKER_OPTS="-H tcp://127.0.0.1:2375 -H unix:///var/run/docker.sock"`

Finally you can run the tests:

```sh
./gradlew test
```

