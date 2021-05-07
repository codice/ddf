## Getting Started

### Prerequisites
* Docker container memory allocation > 12GB (for instance)

### Installation

#### Simple Build
```
$ mvn clean install -Pdocker
$ docker run codice/ddf-docker:${PROJECT_VERSION}
# replace ${PROJECT_VERSION} with correct version (i.e. 2.28.0-SNAPSHOT)
```

#### With ddf-solrcloud
```
# 1. Change custom.system.properties file for the following lines:
solr.cloud.zookeeper=zoo:2181
solr.http.protocol=http
solr.http.url=_DO_NOT_EXPAND_${solr.http.protocol}://solr1:_DO_NOT_EXPAND_${solr.http.port}/solr
# Note: solr1 is the docker container name for solr

# 2. Add the following lines to Dockerfile in ddf-docker
ENV SOLR_ZK_HOSTS=zoo:2181

# 3. Update docker-compose.yml for solrcloud for the following lines:
  solr:
    environment:
      # Replace ${SOLR_CONTAINER} with appropriate name (i.e.: solr1)
      - SOLR_HOST=${SOLR_CONTAINER}

# 4. Build DDF

# 5. Then the below command should be ran:
# run solrcloud
$ cd ${DDF_HOME}/distribution/docker/solrcloud
$ mvn exec:exec@up
# run ddf-docker
$ docker run --network solrcloud_solrcloud -p 0.0.0.0:8993:8993 -d --name ddf codice/ddf-docker:${PROJECT_VERSION}
```

#### With Karaf Debug Mode
```
# 1. Add the following to Dockerfile
ENV KARAF_DEBUG=true
ENV JAVA_DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:5005"
EXPOSE 5005

# 2. Run the container using the following command
$ docker run -p 0.0.0.0:5005:5005 -p 0.0.0.0:8993:8993 --name ddf codice/ddf:${PROJECT_VERSION}

# 3. Then in your IDE or editor, attach to the remote debugger at `localhost:5005`
```

