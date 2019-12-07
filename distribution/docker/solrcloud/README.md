# DDF SolrCloud Docker Compose
This docker compose will start one Zookeeper and two DDF compatible SolrCloud nodes exposed to the local host.

## How to use this

Initial setup:
```
docker network create --subnet=172.28.0.0/16 solr 
```

To run:
```
docker-compose up
```

Services are available locally at:
* zoo.localhost:2181
* <http://solr1.localhost:8994/solr/>
* <http://solr2.localhost:8995/solr/>

To remove volume data:
```
docker-compose down
docker volume rm solrcloud_solr1_data solrcloud_solr2_data solrcloud_zoo_data
```