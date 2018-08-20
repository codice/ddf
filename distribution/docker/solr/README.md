# DDF Compatible Solr Container

This docker image simply builds on top of the official solr image and adds in a few components to make it fully compatible with DDF

## Usage

To use this image, run: `docker container run -d -p 8994:8983 codice/ddf-solr:<ddf-version>`

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