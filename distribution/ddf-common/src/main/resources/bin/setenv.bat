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

rem Set environment variable for DDF home directory (used by DDF Java code)
set DDF_HOME=%~dp0%..
set DDF_HOME=%DDF_HOME:\=/%
set DDF_HOME=%DDF_HOME:/bin/..=/%

rem
rem The following section shows the possible configuration options for the default 
rem karaf scripts
rem
rem Window name of the windows console
set KARAF_TITLE=${command.prompt.title}
rem Minimum memory for the JVM
rem SET JAVA_MIN_MEM=2g
rem Maximum memory for the JVM
rem SET JAVA_MAX_MEM=4g
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
rem SET KARAF_OPTS=
rem Uncomment out the line below to enable cxf logging interceptors
rem set EXTRA_JAVA_OPTS="%EXTRA_JAVA_OPTS% -Dcom.sun.xml.ws.transport.http.HttpAdapter.dump=true"

set DDF_HOME_PERM=%DDF_HOME:/=\%
set DDF_HOME_PERM=%DDF_HOME_PERM:\bin\..=\%

rem The following defines an environment variable referencing our script to be executed by the JVM
rem when errors are detected. Unfortunately, forking the error process from Java does not expand
rem variables as it does on Linux. Because the environment variable will be expanded by the batch
rem script and not by Java, we are unable to specify the %p or any other arguments to the ddf_on_error.bat
rem script as it requires injecting a space and variable expansions occuring in 2 places in karaf.bat
rem renders it impossible to do so in a safe way no matter how we try to escape the space
rem As a work around, we will have the ddf_on_error.bat script use the karaf.pid file that gets created
rem and it will default to being invoked via the 'script'
rem The space would be important at the end of the DDF_ON_ERROR value to separate the last argument from
rem the %p used by the JVM to represent the pid of the JVM
rem make sure there is a space after the ^
rem set DDF_ON_ERROR=bin\ddf_on_error.bat script^
set DDF_ON_ERROR=bin\ddf_on_error.bat

rem Defines the special on-error Java options
rem set JAVA_ERROR_OPTS=-XX:OnOutOfMemoryError=%DDF_ON_ERROR%%%p -XX:OnError=%DDF_ON_ERROR%%%p
set JAVA_ERROR_OPTS=-XX:OnOutOfMemoryError=%DDF_ON_ERROR% -XX:OnError=%DDF_ON_ERROR%
set KARAF_OPTS=-Dfile.encoding=UTF8
set JAVA_OPTS=-Xms2g -Xmx6g -Dderby.system.home="%DDF_HOME%\data\derby" -Dderby.storage.fileSyncTransactionLog=true -Dfile.encoding=UTF8 -Dddf.home=%DDF_HOME% -Dddf.home.perm=%DDF_HOME_PERM% -XX:+DisableAttachMechanism %JAVA_ERROR_OPTS%