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
<img src="https://tools.codice.org/wiki/download/attachments/1179800/ddf.jpg"/>

## STANAG 4559 Mock Server

DDF contains a STANAG 4559 compliant mock server that can be used for development purposes.  This package contains a mock server and sample client that can be run by executing the following commands from ddf/distribution/sdk/sample-stanag4559-server/ (Substitute the WebPort and CorbaPort with available ports on your machine).


```
mvn -Pcorba.server -Dexec.args=WebPort,CorbaPort
```

```
mvn -Pcorba.client -Dexec.args=WebPort
```

## Copyright / License
Copyright (c) Codice Foundation
 
This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License 
as published by the Free Software Foundation, either version 3 of the License, or any later version. 
 
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
<http://www.gnu.org/licenses/lgpl.html>.
 
