/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

require('es6-promise').polyfill()
require('isomorphic-fetch')

// retrieves logs from the endpoint
export const getLogs = done => {
  const endpoint =
    './jolokia/exec/org.codice.ddf.platform.logging.LoggingService:service=logging-service/retrieveLogEvents'

  window
    .fetch(endpoint, {
      credentials: 'same-origin',
      headers: {
        'X-Requested-With': 'XMLHttpRequest',
      },
    })
    .then(res => res.json())
    .then(json => done(null, json.value))
    .catch(done)
}
