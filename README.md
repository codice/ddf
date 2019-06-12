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

<img src="http://www.codice.org/ddf/images/ddf_logo.png"/>

# [Distributed Data Framework \(DDF\)](http://ddf.codice.org/)
[![CLA assistant](https://cla-assistant.io/readme/badge/codice/ddf)](https://cla-assistant.io/codice/ddf)
[![pullreminders](https://pullreminders.com/badge.svg)](https://pullreminders.com?ref=badge)


Distributed Data Framework (DDF) is an open source, modular integration framework. 

## Features
 * Standardization
    - Building on established Free and Open Source Software (FOSS) and open standards avoids vendor lock-in
 * Extensibility
    - Capabilities can be extended by developing and sharing new features
    - Built on top of Apache Karaf for OSGi support
    - Apache Camel and Apache CXF integration
 * Flexibility
    - Only features required can be deployed
 * Federated Open Geospatial Consortium (OGC) filter powered metadata catalog
     - Enterprise Service Bus (ESB) performs transformation between query standards
     - Apache Solr integration
        - Well Known Text (WKT) indexing and search for spatial awareness
        - Full text search
        - XPath searches
        - XML indexing
     - Open Geospatial Consortium (OGC) KML, CSW, and WFS federated services
     - OpenSearch federated services
     - REST API for catalog operations
     - Integrated content framework to store actual products associated with the indexed metadata
     - Tika parser for extracting metadata from common file formats (Office, PDF, etc)
     - Plugin support for pre and post processing on all operations
     - Eventing for notifications
     - Metrics
 * Security
     - Web Service Security (WSS) functionality that comes with DDF is integrated throughout the system
         - SAML 2.0 Web Browser SSO Profile with included IdP server and client
         - SAML ECP Profile
         - SAML Security Token Service (STS) based on WS-Trust
         - Automatic protection and Single Sign On (SSO) for web applications without modifying the application itself
         - Extensible PDP with XACML 3.0 support for authorization decisions
         - LDAP integration
             - Included OSGi enabled OpenDJ LDAP server
         - X.509 authentication
         - Basic authentication
         - SAML authentication
         - Guest login support
         - WS-Security, WS-SecurityPolicy, WS-Policy, WS-Trust, WS-SecureConversation, WS-Addressing
     - Provides a pluggable and extensible Security Framework (a set of APIs that define the integration with the DDF framework)
     - Provides Security Service reference implementations for a realistic end-to-end use case.
     - Role and Attribute based access control
     - Attribute based filtering for searches performed throughout the system
     - Federated identity through metadata catalog
 * Search user interface
    - 3D WebGL map based on Cesium
    - 2D map based on OpenLayers 3
    - USNG/MGRS grid support
    - GeoNames geocoder integrated into both maps
    - CometD integration for push notifications
    - Upload and edit capability
    - Saved workspaces (searches and metadata artifacts)
 * Admin Web user interface
    - Web based install wizard
    - Application grid to organize configurations
    - Pluggable configuration pages for applications to simplify configurations for complex scenarios
    - Metrics web application to view up to date system metrics
 * Simplicity of installation and operation
    - Unzip and run
    - Configuration and Installation via a modern Admin Web console
 * Simplicity of Development
    - Build simple Java Objects and wire them in via a choice of dependency injection frameworks
    - Make use of widely available documentation and components for DDF's underlying technologies
    - Modular development supports multi-organizational and multi-regional teams
 
## Building
### What you need ###
* [Install J2SE 8 SDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
* Make sure that your JAVA\_HOME environment variable is set to the newly installed JDK location, and that your PATH includes %JAVA\_HOME%\bin (Windows) or $JAVA\_HOME$/bin (\*NIX).
* [Install Maven 3.1.0 \(or later\)](http://maven.apache.org/download.html). Make sure that your PATH includes the MVN\_HOME/bin directory.
* Set the MAVEN_OPTS variable with the appropriate memory settings
### Optional 
* If you do not wish to run formatting from the commandline (see below) you may use an IDE to format the code for you with the google-java-format plugins.
  - https://github.com/google/google-java-format
    * IntelliJ: https://plugins.jetbrains.com/plugin/8527
    * Eclipse: https://github.com/google/google-java-format/releases/download/google-java-format-1.3/google-java-format-eclipse-plugin-1.3.0.jar



### How to build ###
In order to run through a full build, be sure to have a clone for the ddf repository and optionally the ddf-support repository (NOTE: daily snapshots are deployed so downloading and building each repo may not be necessary since those artifacts will be retrieved.):

```
git clone git://github.com/codice/ddf.git
git clone git://github.com/codice/ddf-support.git (Optional)
```
Change to the root directory of the cloned ddf repository. Run the following command:

```
mvn install
```

This will compile DDF and run all of the tests in the DDF source distribution. It usually takes some time for maven to download required dependencies in the first build.
The distribution will be available under "distribution/ddf/target" directory.

#### Formatting
If during development the build fails for formatting, eg:
```
Failed to execute goal com.coveo:fmt-maven-plugin:1.8.0:check (default-cli) on project <module name>: Found <number> non-complying files, failing build
 ```
You can run the formatter with the command:
```
mvn fmt:format
```


For additional information about building DDF, see [Building DDF](https://codice.atlassian.net/wiki/spaces/DDF/pages/70986756).

## How to Run
* Unzip the distribution. 
* Run the executable at <distribution_home>/bin/ddf.bat or <distribution_home>/bin/ddf

## Additional information
Find documentation about DDF at [codice.org](http://codice.org/ddf/Documentation-versions.html).

Discussions can be found on the [Announcements forum](http://groups.google.com/group/ddf-announcements),  [Users forum](http://groups.google.com/group/ddf-users), and  [Developers forum](http://groups.google.com/group/ddf-developers).

For changes between versions and known issues, please read the [release notes](https://codice.atlassian.net/wiki/spaces/DDF/pages/71275152/Release+Notes).

If you find any issues with DDF, please submit an [issue](https://github.com/codice/ddf/issues).

For information on contributing to DDF see [Contributing to Codice](http://www.codice.org/contributing).

The DDF Website also contains additional information at [http://ddf.codice.org](http://ddf.codice.org).

Many thanks for using DDF.

-- The Codice DDF Development Team

## Copyright / License
Copyright (c) Codice Foundation
 
This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License 
as published by the Free Software Foundation, either version 3 of the License, or any later version. 
 
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
<http://www.gnu.org/licenses/lgpl.html>.
 
