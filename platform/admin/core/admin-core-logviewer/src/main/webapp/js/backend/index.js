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

import eventStream from 'event-stream'
import concat from 'concat-stream'
import http from 'http'

// retrieves logs from the endpoint
const getLogs = function (done) {
  const endpoint = '/admin/jolokia/exec/org.codice.ddf.platform.logging.LoggingService:service=logging-service/retrieveLogEvents'

  http.get({
    path: endpoint
  }, function (res) {
    res.pipe(concat(function (body) {
      try {
        done(null, JSON.parse(body))
      } catch (e) {
        done(e)
      }
    }))
  })
}

// polls the endpoint in batches of 500 and streams them out individually
export default () => {
  var logs = []

  return eventStream.readable(function (count, next) {
    if (logs.length > 0) {
      if (count > 500) {
        // set polling interval
        setTimeout(function () {
          next(null, logs.shift())
        }, 10)
      } else {
        next(null, logs.shift())
      }

    // fetch new logs if all have been streamed out
    } else {
      getLogs(function (err, body) {
        if (err) {
          next(err)
        } else {
          logs = body.value
          next(null, logs.shift())
        }
      })
    }
  })
}
