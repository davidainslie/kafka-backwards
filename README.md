Kafka in Scala
==============

Installation requirements
-------------------------
On Mac:
```
$ brew update && brew install scala && brew install sbt && brew install kubernetes-cli && brew install kubectl && brew cask install virtualbox docker minikube
```

Unit Testing
------------
```
$ sbt test
```

Integration Testing
-------------------
```
$ sbt it:test
```

Gatling Testing
---------------
Gatling tests can act as performance tests and acceptance tests (thus providing regression tests).
```
$ sbt gatling-it:test
```

Acceptance Testing
------------------
```
$ sbt acceptance:test
```

Release
-------
```
$ sbt "release with-defaults"
```

Run Application Locally
-----------------------
Within the root directory of this project we can run the application using "sbt run".
However, as the application creates directories/files running outside a Docker container will cause issues in a local environment.
In this case we want to only create necessary directories and files within a "temp" directory in this project.
That being the case, then run locally with the following, noting the use of the "integration testing configuration":
```
$ sbt '; set javaOptions += "-Dconfig.file=./src/main/resources/application.local.conf"; run'
```

Docker Compose
--------------
Using "sbt docker compose" plugin.

To use locally built images for all services defined in the Docker Compose file instead of pulling from the Docker Registry use the following command:

```
$ sbt "dockerComposeUp skipPull"
```

To shutdown all instances started from the current project with the Plugin enabled run:

```
$  sbt dockerComposeStop
```

The docker-compose.yml (under ./docker) boot all necessary services (Zookeeper, Kafka) and the service provided by this module.

To boot more than one Kafka, there is the script docker-compose.sh (under ./docker) which starts 3 Kafka brokers - note this script must be executable:
```
$ chmod u+x docker-compose.sh
$ ./docker-compose.sh
```

An easy way to validate Kafka is to create a test publisher and consumer using **kafkacat** - again on Mac:
```
$ brew install kafkacat
```

Then start a test publisher (in on terminal) and publish events to kafka by:
```
$ kafkacat -P -b <host>:<port> -t test
scooby
doo
```

Which can be consumed (in another terminal) by:
```
$ kafkacat -C -b <host>:<port> -t test
scooby
doo
```

where "host" and "port" can be acquired from "docker" - run the following:
```
$ docker ps -a

CONTAINER ID        IMAGE                    COMMAND                  CREATED             STATUS              PORTS                                                NAMES
4d4393efbee4        wurstmeister/kafka       "start-kafka.sh"         57 seconds ago      Up 56 seconds       0.0.0.0:32782->9092/tcp                              docker_kafka_3
276c231611b8        wurstmeister/zookeeper   "/bin/sh -c '/usr/sb…"   57 seconds ago      Up 56 seconds       22/tcp, 2888/tcp, 3888/tcp, 0.0.0.0:2181->2181/tcp   docker_zookeeper_1
e3d41ce77ac0        wurstmeister/kafka       "start-kafka.sh"         57 seconds ago      Up 56 seconds       0.0.0.0:32781->9092/tcp                              docker_kafka_1
9bb17021a60b        wurstmeister/kafka       "start-kafka.sh"         57 seconds ago      Up 56 seconds       0.0.0.0:32780->9092/tcp                              docker_kafka_2
```

The example output shows that we can publish/consume our tests on e.g. 0.0.0.0:32782