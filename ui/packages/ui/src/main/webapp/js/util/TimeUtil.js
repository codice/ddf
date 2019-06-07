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
     * http://stackoverflow.com/questions/1787939/check-time-difference-in-javascript
     * @param nTotalDiff
     * @returns {string}
     */
    convertUptimeToString: function(uptime) {
      var oDiff = {}

      oDiff.days = Math.floor(uptime / 1000 / 60 / 60 / 24)
      uptime -= oDiff.days * 1000 * 60 * 60 * 24

      oDiff.hours = Math.floor(uptime / 1000 / 60 / 60)
      uptime -= oDiff.hours * 1000 * 60 * 60

      oDiff.minutes = Math.floor(uptime / 1000 / 60)
      uptime -= oDiff.minutes * 1000 * 60

      oDiff.seconds = Math.floor(uptime / 1000)
      //  -------------------------------------------------------------------  //

      //  Format Duration
      //  -------------------------------------------------------------------  //
      //  Format Hours
      var daystext = String(oDiff.days)

      var hourtext = '00'
      if (oDiff.hours > 0) {
        hourtext = String(oDiff.hours)
      }
      if (hourtext.length === 1) {
        hourtext = '0' + hourtext
      }

      //  Format Minutes
      var mintext = '00'
      if (oDiff.minutes > 0) {
        mintext = String(oDiff.minutes)
      }
      if (mintext.length === 1) {
        mintext = '0' + mintext
      }

      //  Format Seconds
      var sectext = '00'
      if (oDiff.seconds > 0) {
        sectext = String(oDiff.seconds)
      }
      if (sectext.length === 1) {
        sectext = '0' + sectext
      }

      //  Set Duration
      var sDuration = daystext + 'd ' + hourtext + ':' + mintext + ':' + sectext
      //  -------------------------------------------------------------------  //

      return sDuration
    },
  }

  return Util
})
