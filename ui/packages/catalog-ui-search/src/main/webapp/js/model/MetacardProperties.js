/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
const Backbone = require('backbone')
const _ = require('underscore')
const metacardDefinitions = require('../../component/singletons/metacard-definitions.js')
const TurfMeta = require('@turf/meta')
const wkx = require('wkx')
const properties = require('../properties.js')
require('backbone-associations')

module.exports = Backbone.AssociatedModel.extend({
  defaults() {
    return {
      'metacard-tags': ['resource'],
    }
  },
  hasGeometry(attribute) {
    return (
      _.filter(
        this.toJSON(),
        (value, key) =>
          (attribute === undefined || attribute === key) &&
          metacardDefinitions.metacardTypes[key] &&
          metacardDefinitions.metacardTypes[key].type === 'GEOMETRY'
      ).length > 0
    )
  },
  getCombinedGeoJSON() {
    return
  },
  getPoints(attribute) {
    return this.getGeometries(attribute).reduce(
      (pointArray, wkt) =>
        pointArray.concat(
          TurfMeta.coordAll(wkx.Geometry.parse(wkt).toGeoJSON())
        ),
      []
    )
  },
  getGeometries(attribute) {
    return _.filter(
      this.toJSON(),
      (value, key) =>
        !properties.isHidden(key) &&
        (attribute === undefined || attribute === key) &&
        metacardDefinitions.metacardTypes[key] &&
        metacardDefinitions.metacardTypes[key].type === 'GEOMETRY'
    )
  },
})
