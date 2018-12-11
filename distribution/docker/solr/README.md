# DDF Compatible Solr Container

This docker image simply builds on top of the official solr image and adds in a few components to make it fully compatible with DDF

## Usage

To use this image, run: `docker container run -d -p 8994:8983 codice/ddf-solr:<ddf-version>`

### Auto-Creating Cores

To create cores at startup this image supports an environment variable `CORES`.
Cores should be pre-created when using solr in standalone mode, as the solr client does not automatically create missing cores.
When using in cloud or distributed mode the `CORES` option should not be used. In this mode the solr client will automatically create cores that are missing.

To create one or more cores simply add this environment variable to the run command: `docker container run -d -p 8994:8983 -e CORES="catalog metacard_cache preferences" codice/ddf-solr:<ddf-version>`.
This example command will start a solr server with three cores already created: `catalog`, `metacard_cache`, and `preferences`

When providing multiple core names in the `CORES` environment variable must be separated by spaces.

### Docker Compose

Running a basic solr container with docker-compose would look something like this:

```
version: '3.7'
services:
  solr:
    image: codice/ddf-solr:<ddf-version>
    # Ports are unnecessary when running ddf and solr in 
    # the same compose deployment, in that scenario ddf can connect to solr
    # via the service name and actual solr port. For example `solr:8983`
    ports: 
      - 8994:8983
    environment:
      # Provide cores to create at startup
      CORES: "catalog metacard_cache alerts preferences subscriptions notification activity workspace"
```

## Extending

For scenarios where additional cores, plugins, or libaries are required, this image can be used as a source via `FROM codice/ddf-solr:<ddf-version>`

To add an additional ddf compatible core(s) a helper script has been included, run `create-ddf-core [cores...]` in a downstream `Dockerfile`

# Details

## Added plugins

* `ddf.platform.solr:solr-xpath`

## Added libraries

* `org.locationtech.jts:jts-core`

## Added helper scripts

* `create-ddf-core [coresName ...]`


# Pushing to docker.io

In order to push the image up to the docker.io/codice/ddf-solr repository a server entry must be added to `settings.xml` for `docker.io`
