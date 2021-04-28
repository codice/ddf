# DDF Docker Container

This docker image is a DDF image built on top of `codice/docker-ddf-base` image.

## Usage

### Prerequisites
* In order to prevent DDF from crashing during startup, make sure to allocate more memory to docker containers (6GB for instance).

### Sample run commands

Just to get this image to run
```sh
$ docker run codice/ddf-docker:${VERSION}
```

---
To use with ddf-solrcloud, change custom.system.properties file for the following lines:
```
#   solr.cloud.zookeeper=zoo:2181
#   solr.http.protocol=http
#   solr.http.url=_DO_NOT_EXPAND_${solr.http.protocol}://solr1:_DO_NOT_EXPAND_${solr.http.port}/solr
```
Then the below command should be ran:
```sh
$ cd ${DDF_HOME}/distribution/docker/solrcloud
$ mvn exec:exec@up
$ docker run --network solrcloud_solrcloud -p 0.0.0.0:8993:8993 -d --name ddf codice/ddf-docker:${PROJECT_VERSION}
```
