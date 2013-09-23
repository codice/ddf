@echo off
rem
rem
rem    Licensed to the Apache Software Foundation (ASF) under one or more
rem    contributor license agreements.  See the NOTICE file distributed with
rem    this work for additional information regarding copyright ownership.
rem    The ASF licenses this file to You under the Apache License, Version 2.0
rem    (the "License"); you may not use this file except in compliance with
rem    the License.  You may obtain a copy of the License at
rem
rem       http://www.apache.org/licenses/LICENSE-2.0
rem
rem    Unless required by applicable law or agreed to in writing, software
rem    distributed under the License is distributed on an "AS IS" BASIS,
rem    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem    See the License for the specific language governing permissions and
rem    limitations under the License.
rem

rem
rem handle specific scripts; the SCRIPT_NAME is exactly the name of the Karaf
rem script; for example karaf.bat, start.bat, stop.bat, admin.bat, client.bat, ...
rem
rem if "%KARAF_SCRIPT%" == "SCRIPT_NAME" (
rem   Actions go here...
rem )

rem
rem general settings which should be applied for all scripts go here; please keep
rem in mind that it is possible that scripts might be executed more than once, e.g.
rem in example of the start script where the start script is executed first and the
rem karaf script afterwards.
rem

set DDF_HOME=%~dp0%..

rem
rem The following section shows the possible configuration options for the default 
rem karaf scripts
rem
rem Window name of the windows console
SET KARAF_TITLE=${command.prompt.title}
rem Minimum memory for the JVM
rem SET JAVA_MIN_MEM
rem Maximum memory for the JVM
rem SET JAVA_MAX_MEM=2048M
rem Minimum perm memory for the JVM
rem SET JAVA_PERM_MEM=128M
rem Maximum memory for the JVM
rem SET JAVA_MAX_PERM_MEM=512M
rem Karaf home folder
rem SET KARAF_HOME
rem Karaf data folder
rem SET KARAF_DATA
rem Karaf base folder
rem SET KARAF_BASE
rem Additional available Karaf options
rem SET KARAF_OPTS=-Dderby.system.home="..\data\derby"  -Dderby.storage.fileSyncTransactionLog=true -Dcom.sun.management.jmxremote -Dfile.encoding=UTF8 -Dddf.home=%DDF_HOME%

set JAVA_OPTS=-server -XX:PermSize=128m -XX:MaxPermSize=512m -Xmx2048M -Dderby.system.home="%DDF_HOME%\data\derby"  -Dderby.storage.fileSyncTransactionLog=true -Dcom.sun.management.jmxremote -Dfile.encoding=UTF8 -Dddf.home=%DDF_HOME%
:: set JAVA_OPTS=-server -XX:PermSize=128m -XX:MaxPermSize=512m -Xmx2048M -Dfile.encoding=UTF8 -Djavax.net.ssl.keyStore=../etc/keystores/serverKeystore.jks -Djavax.net.ssl.keyStorePassword=changeit -Djavax.net.ssl.trustStore=../etc/keystores/serverTruststore.jks -Djavax.net.ssl.trustStorePassword=changeit -Dddf.home=%DDF_HOME%
