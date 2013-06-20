#
# Copyright (c) Codice Foundation
#
# This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
# version 3 of the License, or any later version. 
#
# This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
# <http://www.gnu.org/licenses/lgpl.html>.
#
KEYSTORE=\"$CATALINA_HOME/certs/keystore.jks\"
TRUSTSTORE=\"$CATALINA_HOME/certs/keystore.jks\" 
PASSWORD=changeit
CATALINA_OPTS=$CATALINA_OPTS" -Djavax.net.ssl.keyStore=$KEYSTORE -Djavax.net.ssl.keyStoreType=JKS -Djavax.net.ssl.keyStorePassword=$PASSWORD"
CATALINA_OPTS=$CATALINA_OPTS" -Djavax.net.ssl.trustStore=$TRUSTSTORE -Djavax.net.ssl.trustStoreType=JKS -Djavax.net.ssl.trustStorePassword=$PASSWORD"
 
export CATALINA_OPTS