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
<img src="https://tools.codice.org/wiki/download/attachments/3047458/ddf.jpg"/>

# [Distributed Data Framework \(DDF\)](http://ddf.codice.org/)
[![Build Status](https://travis-ci.org/codice/ddf.png)](https://travis-ci.org/codice/ddf)


Distributed Data Framework (DDF) is an open source, modular integration framework. 

## Advantages
 * Standardization
    - Building on established Free and Open Source Software (FOSS) and open standards avoids vendor lock-in
 * Extensibility
    - Capabilities can be extended by developing and sharing new features
 * Flexibility
    - Only features required can be deployed
 * Simplicity of installation and operation
    - Unzip and run
    - Configuration via a Web console
 * Simplicity of Development
    - Build simple Java Objects and wire them in via a choice of dependency injection frameworks
    - Make use of widely available documentation and components for DDF's underlying technologies
    - Modular development supports multi-organizational and multi-regional teams
 * Security
    - Web Service Security (WSS) functionality that comes with DDF is integrated throughout the system
    - Provides a Security Framework (a set of APIs that define the integration with the DDF framework)
    - Provides Security Service reference implementations for a realistic end-to-end use case.
 
## Building
### What you need ###
* [Install J2SE 7 SDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html). The build is also compatible with JDK 6.0 Update 29 \(or later\).
* Make sure that your JAVA\_HOME environment variable is set to the newly installed JDK location, and that your PATH includes %JAVA\_HOME%\bin (Windows) or $JAVA\_HOME$/bin (\*NIX).
* [Install Maven 3.0.3 \(or later\)](http://maven.apache.org/download.html). Make sure that your PATH includes the MVN\_HOME/bin directory.
* Set the MAVEN_OPTS variable with the appropriate memory settings
* The DDF Eclipse Code Formatter - [ddf-eclipse-code-formatter.xml](https://github.com/codice/ddf/blob/master/support/support-checkstyle/src/main/resources/ddf-eclipse-code-formatter.xml)

*NIX
```
export MAVEN_OPTS='-Xmx512m -XX:MaxPermSize=128m'
```
Windows
```
set MAVEN_OPTS='-Xmx512m -XX:MaxPermSize=128m'
```


### How to build ###
```
git clone git://github.com/codice/ddf.git
```
Change to the top level directory of DDF source distribution.

```
mvn install
```

This will compile DDF and run all of the tests in the DDF source distribution. It usually takes some time for maven to download required dependencies in the first build.
The distribution will be available under "distribution/ddf/target" directory.

## How to Run
* Unzip the distribution. 
* Run the executable at <distribution_home>/bin/ddf.bat or <distribution_home>/bin/ddf

## Additional information
The [wiki](https://tools.codice.org/wiki/display/DDF) is the right place to find any documentation about DDF.

Discussions can be found on the [Announcements forum](http://groups.google.com/group/ddf-announcements),  [Users forum](http://groups.google.com/group/ddf-users), and  [Developers forum](http://groups.google.com/group/ddf-developers).

For a DDF binary distribution, please read  the release notes on the wiki for a list of supported and unsupported features.

If you find any issues with DDF, please submit reports with [JIRA](https://tools.codice.org/jira/browse/DDF).

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
 
