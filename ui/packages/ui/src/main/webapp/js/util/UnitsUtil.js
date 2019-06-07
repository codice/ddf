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

define([], function() {
  var Util = {
    /**
     * Solution taken from stackoverflow. Link included.
     * http://stackoverflow.com/questions/15900485/correct-way-to-convert-size-in-bytes-to-kb-mb-gb-in-javascript
     * @param bytes - what to convert
     * @returns {string}
     */
    convertBytesToDisplay: function(bytes) {
      if (bytes >= 1000000000) {
        bytes = (bytes / 1000000000).toFixed(2) + ' GB'
      } else if (bytes >= 1000000) {
        bytes = (bytes / 1000000).toFixed(2) + ' MB'
      } else if (bytes >= 1000) {
        bytes = (bytes / 1000).toFixed(2) + ' KB'
      } else if (bytes > 1) {
        bytes = bytes + ' bytes'
      } else if (bytes === 1) {
        bytes = bytes + ' byte'
      } else {
        bytes = '0 byte'
      }
      return bytes
    },
  }

  return Util
})
