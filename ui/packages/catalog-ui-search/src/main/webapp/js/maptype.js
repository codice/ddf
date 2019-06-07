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

const _ = require('underscore')
const webgl = require('./webglcheck.js')
const twoD = require('./2dmapcheck.js')
const qs = require('querystring')
const user = require('../component/singletons/user-instance.js')

function getActiveVisualization() {
  return user
    .get('user')
    .get('preferences')
    .get('visualization')
}

const MapTypeEnum = {
  THREED: '3d',
  TWOD: '2d',
  NONE: 'none',
}

const url = function() {
  // replace removes leading ? in query string
  const query = window.location.search.replace(/^\?/, '')
  return qs.parse(query)
}

module.exports = {
  type: (function() {
    const param = url().map
    if (!_.isUndefined(param)) {
      if (_.contains(_.values(MapTypeEnum), param)) {
        return param
      }
    }

    if (webgl.isAvailable()) {
      return MapTypeEnum.THREED
    } else if (twoD.isAvailable()) {
      return MapTypeEnum.TWOD
    } else {
      return MapTypeEnum.NONE
    }
  })(),

  is3d() {
    return getActiveVisualization() === '3dmap'
  },
  is2d() {
    return getActiveVisualization() === '2dmap'
  },
  isNone() {
    return this.type === MapTypeEnum.NONE
  },
  isMap() {
    return this.is3d() || this.is2d()
  },
}
