# DDF Compatible Solr Cloud Containers

This docker compose build two images: a zookeeper and an official solr image
The solr image has a few add-on components to make it fully compatible with DDF

## Usage

To run: `docker-compose up`

## Running DDF in Cloud mode

Prior to start up DDF, edit the custom.system.properties file

```
start.solr=false
solr.client=CloudSolrClient
solr.cloud.zookeeper=localhost:7771
solr.cloud.maxShardPerNode=4
```

DDF will create all the associate cores if not already exist on the solr cloud instance.

## Extending

For scenarios where additional cores, plugins, or libaries are required, this image can be used as a source via `FROM codice/ddf-solr:<ddf-version>`

# Details

## Added plugins

* `ddf.platform.solr:solr-xpath`

## Added libraries

* `org.locationtech.jts:jts-core`
