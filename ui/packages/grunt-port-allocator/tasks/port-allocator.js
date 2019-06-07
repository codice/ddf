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
var allocator = require('../lib/port-allocator.js')

module.exports = function(grunt) {
  grunt.registerTask('port:allocator', function() {
    grunt.config.requires('ports')

    var done = this.async()

    allocator(function(err, ports) {
      if (err) {
        grunt.fail.fatal(err)
      } else {
        var mapToPorts = function(obj) {
          return Object.keys(obj).reduce(function(o, key, i) {
            o[key] = ports[i]
            return o
          }, {})
        }

        grunt.config('ports', mapToPorts(grunt.config('ports')))

        done()
      }
    })
  })
}
