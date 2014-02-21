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

# [HTTP Proxy](https://tools.codice.org/wiki/display/DDF/DDF+HTTP+Proxy)


## Introduction
This project uses sets up an http/https proxy using Apache Camel inside an OSGi bundle that can be run on an OSGi 4.2+ container.

Additionally, it builds a feature application (kar) that can be deployed to an Apache Karaf (http://karaf.apache.org/) server which contains all of the necessary dependencies. This application install has been tested with Apache Karaf 2.3.2.

## Building
### What you need ###
* [Install J2SE 7 SDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html). The build is also compatible with JDK 6.0 Update 29 \(or later\).
* Make sure that your JAVA\_HOME environment variable is set to the newly installed JDK location, and that your PATH includes %JAVA\_HOME%\bin (Windows) or $JAVA\_HOME$/bin (\*NIX).
* [Install Maven 3.0.3 \(or later\)](http://maven.apache.org/download.html). Make sure that your PATH includes the MVN\_HOME/bin directory.
* Set the MAVEN_OPTS variable with the appropriate memory settings

### How to build ###
```
git clone https://github.com/codice/ddf-http-proxy.git
```
Change to the top level directory of http-proxy source distribution.

```
mvn clean install
```

## Additional information
The [wiki](https://tools.codice.org/wiki/display/DDF/DDF+HTTP+Proxy) page contains configuration information on setting up the HTTP Proxy.

Discussions can be found on the DDF area -- [Announcements forum](http://groups.google.com/group/ddf-announcements),  [Users forum](http://groups.google.com/group/ddf-users), and  [Developers forum](http://groups.google.com/group/ddf-developers).

If you find any issues, please submit reports with [JIRA](https://tools.codice.org/jira/browse/DDF).

For information on contributing see [Contributing to Codice](http://www.codice.org/contributing).

The DDF Website also contains additional information at [http://ddf.codice.org](http://ddf.codice.org).

-- The Codice Development Team

## Copyright / License
Copyright (c) Codice Foundation
 
This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License 
as published by the Free Software Foundation, either version 3 of the License, or any later version. 
 
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
<http://www.gnu.org/licenses/lgpl.html>.
 
