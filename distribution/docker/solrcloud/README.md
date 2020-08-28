# DDF SolrCloud Docker Compose
Docker compose will start one Zookeeper and two DDF compatible SolrCloud nodes exposed to the local host.

## How to use this

Note: You may have to enable file sharing in Docker with this directory on Windows or MacOS.

Services are available locally at:
* Zookeeper at localhost:2181
* <http://localhost:8994/solr/>
* <http://localhost:8995/solr/>

To run:
```
mvn exec:exec@up
```

To see logs:
```
mvn exec:exec@logs
```

To stop:
```
mvn exec:exec@down
```

To remove volume data:
```
mvn exec:exec@rm
```