# This Dockerfile uses multi-stage builds. The prep stage is used
# to prepare any needed files for use in the final image. This avoids 
# having to do cleanup inside of steps in the final image.

# Prep Stage
FROM alpine as prep

RUN apk add --no-cache unzip
RUN mkdir /prep /zips
COPY maven/ddf-${project.version}.zip /zips
RUN unzip /zips/ddf-${project.version}.zip -d /prep
RUN mv /prep/ddf-${project.version} /prep/ddf

# Final Image Stage
FROM codice/ddf-base:${docker.ddf.base.version}
LABEL maintainer=codice

ENV APP_HOME=/app
ENV APP_NAME=/ddf
ENV APP_LOG=$APP_HOME/data/log/$APP_NAME.log

COPY --from=prep /prep/ddf $APP_HOME

VOLUME $APP_HOME/data $APP_HOME/deploy $APP_HOME/etc

EXPOSE 8993 8181 8101
