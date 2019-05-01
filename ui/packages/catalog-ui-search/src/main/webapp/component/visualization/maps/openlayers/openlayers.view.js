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

var MapView = require('../map.view')
var wreqr = require('../../../../js/wreqr.js')
var $ = require('jquery')

module.exports = MapView.extend({
  className: 'is-openlayers',
  loadMap: function() {
    var deferred = new $.Deferred()
    require(['./map.openlayers'], function(OpenlayersMap) {
      deferred.resolve(OpenlayersMap)
    })
    return deferred
  },
})
