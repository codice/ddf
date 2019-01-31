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
This module is a bundled distribution of the GeoWebCache war. GeoWebCache is a server providing a tile cache and tile service aggregation.  See (http://geowebcache.org) for more information.  This application also provides an administrative plugin for the management of GeoWebCached layers.  GeoWebCache also provides a user interface that can be used to preview layers and truncate or seed them (https://localhost:8993/geowebcache/).

By installing this application in DDF, all of the GeoWebCache capabilities are available running in the DDF container.  This includes
- WMS/WMTS endpoints (/geowebcache/service)
- Connections to external tile services configured via RESTful services (/geowebcache/rest)
- Caching of tiles
- Seeding of tiles by zoom level
- An Admin Plugin to configure tiles

## Usage

### Configure GWC layers
GWC needs to be configured to utilize one or more *backend* tile services which are exposed via a service endpoint that ${catalog-search-ui} can utilize.  The following XML shows the minimum amount of information required to configure a WMS layer:

states.xml:
```
    <wmsLayer>
      <name>states</name>
      <mimeFormats>
        <string>image/png</string>
      </mimeFormats>
      <wmsUrl>
        <string>http://demo.opengeo.org/geoserver/topp/wms</string>
      </wmsUrl>
    </wmsLayer>
```

The GeoWebCache Admin Plugin will present a table with all configured layers, as well as the ability to add, remove or update them.

The current layer configurations can also be viewed at the following URL:
`https://localhost:8993/geowebcache/rest/layers`

To add a layer, the minimum information (shown above) can be configured through the `GeoWebCache Layers` tab within the GWC App.  The following modal shows how the information is entered.  Optionally, one can add the name(s) of WMS layers that exist at the URL specified. If no WMS Layer names are specified, GeoWebCache will look for the Layer Name specified in the name field.  Otherwise, it will attempt to find all layer names added under the WMS Layers section, and combine them into one layer.
<img src="https://codice.atlassian.net/wiki/download/attachments/1179800/gwcAddLayer"/>

**Note: When changing the WMS Layers within a GeoWebCache Layer, it is recommended to truncate the layer to remove old tile images.**

Alternatively, the layer XML can be posted with CURL :
`curl -v -k -XPUT -H "Content-type: text/xml" -d @states.xml "https://localhost:8993/geowebcache/rest/layers/states.xml"`

To update a configuration with CURL :
`curl -v -k -X POST -H "Content-type: text/xml" -d @states.xml "https://localhost:8993/geowebcache/rest/layers/states.xml"`

GeoWebCache also provides a user interface that can be used to preview layers and truncate or seed them at `https://localhost:8993/geowebcache/`.

### Configure GWC disk quota
> Since disk usage increases geometrically by zoom level, one single seeding task could fill up an entire storage device. Because of this, GeoWebCache employs a disk quota system where one can specify the maximum amount of disk space to use for a particular layer or for the entire set of layers (the “Global Quota”), as well as logic on how to proceed when that quota is reached. There are two different policies for managing the disk quotas: Least Frequently Used (LFU) and Least Recently Used (LRU). 
(https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet#blockquotes)

To view the disk quota XML representative: 
`"https://localhost:8993/geowebcache/rest/diskquota.xml"`

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
