# This Dockerfile uses multi-stage builds. The prep stage is used
# to prepare any needed files for use in the final image. This avoids 
# having to do cleanup inside of steps in the final image.

# Prep Stage
FROM alpine as prep

RUN apk add --no-cache unzip
RUN mkdir /prep jars
COPY maven/solr-schema-${project.version}.jar /jars
RUN unzip /jars/solr-schema-${project.version}.jar -d /prep

# Final Image Stage
FROM solr:${solr.version}
# Copy solr libs
COPY maven/jts-core-1.15.0.jar /opt/solr/server/solr-webapp/webapp/WEB-INF/lib/

# Copy solr plugins
COPY maven/solr-xpath-${project.version}.jar /opt/solr/plugins/

# Copy all solr core configurations to /seed directory
USER root
RUN mkdir /seed
COPY --from=prep /prep/solr/conf/* /seed/
COPY maven/createCore.sh /usr/local/bin/create-ddf-core
RUN chmod 755 /usr/local/bin/create-ddf-core
RUN chown -R solr:solr /seed
USER solr

# Set ENV vars for use by create-ddf-core script
ENV CONFIG_SRC=/seed
ENV CORE_DEST=/opt/solr/server/solr/cores
RUN mkdir -p $CORE_DEST

# Create directory for SolrCloud nodes
ENV NODE_DEST=/opt/solr/server/solr/nodes
RUN mkdir -p $NODE_DEST/node1 $NODE_DEST/node2
RUN chown -R solr:solr $NODE_DEST

# Add ddf-solr entrypoint
COPY maven/docker-ddf-solr-entrypoint.sh /usr/local/bin/docker-ddf-solr-entrypoint
ENTRYPOINT ["/usr/local/bin/docker-ddf-solr-entrypoint"]
CMD ["solr-foreground"]
