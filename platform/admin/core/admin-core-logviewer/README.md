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
# Admin UI LogViewer

## Directory Structure
### src/main/webapp
The top level of this directory contains index.html and any other javascript files that do not pertain to react or redux.
+ `actions`: Redux actions files.
+ `backend`: Files relevant to retrieving log lists from the logging endpoint.
+ `components`: Files that return React view components.
+ `reducers`: Redux reducer files.
+ `css`: CSS files.

### src/test/js
Contains javascript unit tests.

## Tests and Development
### Running Unit tests
To run tests, do:

    npm test

## What is expected from the backend?
The backend is expected to send a JSON object formatted as follows:
    
    {
        "request": {
            "mbean": "example.logging.service",
            "type": "exec",
            "operation": "exampleOperation"
        },
        "value": [
            {
                "level": "INFO",
                "bundleVersion": "1.2.3",
                "bundleName": "example.logged.bundle",
                "message": "Example message",
                "timestamp": 1234567890123
            }
        ]
    }
    
The `value` field contains the list of log items that will be parsed and added.
