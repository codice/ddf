# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

Upstream open-source DDF (Distributed Data Framework) 2.29.32 by Codice Foundation (LGPL v3). This is a vendor dependency — code here is not owned by Connexta. Changes to this repo feed into the local builds of `../ddf-ui` and `../aus`.

Full system context: `/workspace/mark.smith/gits/CLAUDE.md`.

## Subsystems relevant to this workspace

Most of DDF is not touched by the local ISR application. The parts that matter:

### Used by ddf-ui (`../ddf-ui`)

| GroupId | Key artifacts | Role |
|---|---|---|
| `ddf.catalog.core` | `catalog-core-api`, `catalog-core-api-impl`, `catalog-core-commons`, `versioning-api/common` | Core `CatalogFramework`, `Metacard`, `Query`, `Result` types; CQL filter utilities; metacard history |
| `ddf.catalog.rest` | `catalog-rest-api` | JAX-RS interface contracts the ddf-ui backend implements |
| `ddf.catalog.transformer` | `catalog-transformer-propertyjson-metacard`, `geojson-metacard-transformer` | JSON/GeoJSON metacard serialization |
| `ddf.security.core` | `security-core-api`, `security-core-impl` | `SecuritySubject`, attribute extraction |
| `ddf.platform.security` | `security-oidc-bundle`, `session-management-api` | OIDC/OAuth token handling; session lifecycle |
| `ddf.security.servlet` | `security-servlet-logout-api`, `security-servlet-web-socket-api` | Logout and WebSocket auth hooks |
| `ddf.security.storage` | `token-storage-api`, `token-storage-impl` | OAuth token persistence |
| `ddf.platform` | `platform-util`, `platform-configuration-service`, `branding-api`, `preferences-api` | System config, branding, per-user preferences |
| `ddf.action.core` | `action-core-api`, `action-core-impl` | Pluggable metacard action provider pattern |
| `ddf.persistence.core` | `persistence-core-api` | Key-value store for workspace/saved-query persistence |
| `org.codice.ddf.spatial` | `spatial-geocoding-api`, `spatial-geocoding-geocoder` | Place-name geocoding for the map gazetteer |
| `ddf.lib` | `gson-support`, `common-system`, `tlmatcher` | JSON, system properties, template matching |
| `ddf.platform.usng4j` | `platform-usng4j-api` | MGRS/USNG coordinate conversion |
| `ddf.mime.core` | `mime-core-api` | MIME type detection |

### Used by aus (`../aus`)

aus consumes everything ddf-ui uses (above) plus directly:

| GroupId | Key artifacts | Role |
|---|---|---|
| `ddf.catalog.core` | `catalog-core-api`, `filter-proxy`, `catalog-core-attachment` | Catalog framework, query building, content attachments |
| `ddf.security.handler` | `security-handler-impl` | Auth handler chain |
| `ddf.platform.email` | `platform-email-api` | Email notifications |

### Subsystems this workspace does NOT use

Camel/CXF routing internals, Tika input transformers, OpenSearch/CSW/WFS federation, the security IdP server, Solr bundle internals, and most of the admin platform are not directly depended on by aus or ddf-ui.

## Build

Java 17. Standard Maven build — same pattern as other repos in this workspace:
```bash
JAVA_HOME=/usr/lib/jvm/java-17 mvn install
```

This repo is consumed as a local Maven dependency by `../ddf-ui` and `../aus`. Build and install to `~/.m2` before building those repos if you've made changes here.
