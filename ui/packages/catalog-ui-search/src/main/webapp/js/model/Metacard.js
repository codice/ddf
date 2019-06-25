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
const Backbone = require('backbone')
require('backbone-associations')

const MetacardPropertiesModel = require('./MetacardProperties.js')

module.exports = Backbone.AssociatedModel.extend({
  hasGeometry(attribute) {
    return this.get('properties').hasGeometry(attribute)
  },
  getPoints(attribute) {
    return this.get('properties').getPoints(attribute)
  },
  getGeometries(attribute) {
    return this.get('properties').getGeometries(attribute)
  },
  relations: [
    {
      type: Backbone.One,
      key: 'properties',
      relatedModel: MetacardPropertiesModel,
    },
  ],
  defaults: {
    queryId: undefined,
  },
})
