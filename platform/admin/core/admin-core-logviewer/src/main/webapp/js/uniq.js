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
import hash from 'object-hash'

const MAX_COMPARE_SIZE = 5000

// filters out old/duplicate logs and stream out new/unique logs and attaches a hash
export default () => {
  var lastSet = []

  // uses timestamp + message to determine uniqueness
  var isNotInOldList = (currentEntry) => {
    var found = lastSet.find((oldEntry) => {
      return (currentEntry.timestamp === oldEntry.timestamp && currentEntry.message === oldEntry.message)
    })

    return found === undefined
  }

  // generates an MD5 hash from the object and adds it as a key
  var attachHash = (entry) => {
    entry.hash = hash.MD5(entry)
    return entry
  }

  return eventStream.through(function (currentEntry) {
    if (isNotInOldList(currentEntry)) {
      this.emit('data', attachHash(currentEntry))
      lastSet.unshift(currentEntry) // append to front of lastSet
      if (lastSet.length > MAX_COMPARE_SIZE) {
        lastSet.pop() // get rid of oldest one
      }
    }
  })
}
