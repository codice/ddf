# DDF SolrCloud Docker Compose
This docker compose will start one Zookeeper and two DDF compatible SolrCloud nodes exposed to the local host.

## How to use this

Note: You may be asked to enabled file sharing in Docker with this directory on Windows or MacOS.

To run:
```
docker-compose up
```

Services are available locally at:
* Zookeeper at localhost:2181
* <http://localhost:8994/solr/>
* <http://localhost:8995/solr/>

To remove volume data:
```
docker-compose down
docker volume rm solrcloud_solr1_data solrcloud_solr2_data solrcloud_zoo_data
```