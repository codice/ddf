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
  var Metatypes = {}

  return {
    add: function(id, metatypeModel) {
      Metatypes[id] = Metatypes[id] || metatypeModel
    },
    get: function(id) {
      return Metatypes[id]
    },
    getRelevantOptionLabel: function(options) {
      console.log(Metatypes)
      if (
        options.metatypeId === undefined ||
        options.property === undefined ||
        options.value === undefined
      ) {
        throw 'insufficient parameters to determine the relevant label'
      }
      if (this.get(options.metatypeId) === undefined) {
        return ''
      }
      return this.get(options.metatypeId)
        .get(options.property)
        .get('options')[options.value].label
    },
  }
})
