<!--
/*
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
-->

# GeoWebCache 
## Introduction
This module is a a bundled distribution of the GeoWebCache war. GeoWebCache is a server providing a tile cache and tile service aggregation.  See (http://geowebcache.org) for more information.
**The GeoWebCache DDF app is considered experimental.  Basic capabilities have been tested, but full integration into DDF including administration of GeoWebCache is incomplete.**

By installing this application in DDF, all of the GeoWebCache capabilities are available running in the DDF container.  This includes
- WMS/WMTS endpoints (/geowebcache/service)
- Connections to external tile services configured via RESTful services (/geowebcache/rest)
- Caching of tiles
- Seeding of tiles by zoom level

## Building and deploying
GWC in DDF utilizes the GWC 1.5.0 war: http://sourceforge.net/projects/geowebcache/files/geowebcache/1.5.0/geowebcache-1.5.0-war.zip/download
- Download and install GWC war into your maven repo (this will eventually be installed in the codice maven repo)

`mvn install:install-file -Dfile=geowebcache.war -DgroupId=org.geowebcache -DartifactId=geowebcache -Dversion=1.5.0 -Dpackaging=war`

`mvn clean install`

`cp geowebcache-app-target/geowebcache-app-2.9.0-SNAPSHOT.kar <DDF_HOME>/deploy`

## Usage

### Configure GWC layers
GWC needs to be configured to utilize one or more *backend* tile services which are exposed via a service endpoint that the Search UI can utilize.

The current layer configuration can be viewed as follows:
`curl -v -k "https://localhost:8993/geowebcache/rest/layers"`

Detailed layer information can be viewed with:
`curl -v -k "https://localhost:8993/geowebcache/rest/layers/<layerName>.xml"`


To add a layer, the XML representation of the layer can be posted:
`curl -v -k -XPUT -H "Content-type: text/xml" -d @states.xml "https://localhost:8993/geowebcache/rest/layers/states.xml"`

states.xml:
```
    <wmsLayer>
      <name>states</name>
      <mimeFormats>
        <string>image/gif</string>
        <string>image/jpeg</string>
        <string>image/png</string>
        <string>image/png8</string>
      </mimeFormats>
      <wmsUrl>
        <string>http://demo.opengeo.org/geoserver/topp/wms</string>
      </wmsUrl>
    </wmsLayer>
```

### Configure GWC disk quota
> Since disk usage increases geometrically by zoom level, one single seeding task could fill up an entire storage device. Because of this, GeoWebCache employs a disk quota system where one can specify the maximum amount of disk space to use for a particular layer or for the entire set of layers (the “Global Quota”), as well as logic on how to proceed when that quota is reached. There are two different policies for managing the disk quotas: Least Frequently Used (LFU) and Least Recently Used (LRU). 
(https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet#blockquotes)

To view the disk quota XML representative: 
`curl -v -k "https://localhost:8993/geowebcache/rest/diskquota.xml"`

To update the disk quota, a client can post a new XML configuration:
`curl -v -k -XPUT -H "Content-type: text/xml" -d @diskquota.xml "https://localhost:8993/geowebcache/rest/diskquota.xml"`

diskquota.xml:
```
<gwcQuotaConfiguration>
  <enabled>true</enabled>
  <diskBlockSize>2048</diskBlockSize>
  <cacheCleanUpFrequency>5</cacheCleanUpFrequency>
  <cacheCleanUpUnits>SECONDS</cacheCleanUpUnits>
  <maxConcurrentCleanUps>5</maxConcurrentCleanUps>
  <globalExpirationPolicyName>LFU</globalExpirationPolicyName>
  <globalQuota>
    <value>100</value>
    <units>GiB</units>
  </globalQuota>
  <layerQuotas/>
</gwcQuotaConfiguration>
```

See http://geowebcache.org/docs/current/configuration/diskquotas.html for more information on configuration options for disk quota.

### Configure the Standard Search UI
Add a new Imagery Provider in the Standard Search UI Config in the Admin Console (/admin)

`{"type" "WMS" "url" "https://localhost:8993/geowebcache/service/wms" "layers" ["states"]  "alpha" 0.5}`
