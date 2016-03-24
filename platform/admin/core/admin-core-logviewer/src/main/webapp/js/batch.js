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

const DEFAULT_INTERVAL = 250

// adds back-pressure to handle high-volume logs (like TRACE)
export default (interval) => {
  var that
  var buff = []

  const flush = () => {
    if (that !== undefined && buff.length > 0) {
      that.emit('data', buff)
      buff = []
    }
  }

  const i = setInterval(flush, interval || DEFAULT_INTERVAL)

  return eventStream.through(function (data) {
    that = this
    buff.unshift(data)
  }, () => {
    clearInterval(i)
    flush()
    that.emit('end')
  })
}
