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

const _ = require('underscore')
const Backbone = require('backbone')
const PropertyModel = require('../property/property.js')
require('backbone-associations')

module.exports = Backbone.AssociatedModel.extend({
  defaults: {
    value: undefined,
    isValid: true,
    property: undefined,
  },
  relations: [
    {
      type: Backbone.One,
      key: 'property',
      relatedModel: PropertyModel,
    },
  ],
  setValue: function(value) {
    this.set('value', value)
  },
  getValue: function() {
    return this.get('value')
  },
  setIsValid: function(isValid) {
    this.set('isValid', isValid)
  },
  isValid() {
    return this.get('isValid')
  },
  getCalculatedType: function() {
    return this.get('property').getCalculatedType()
  },
  getId: function() {
    return this.get('property').getId()
  },
  isReadOnly: function() {
    return this.get('property').isReadOnly()
  },
  isEditing: function() {
    return this.get('property').isEditing()
  },
  isMultivalued: function() {
    return this.get('property').isMultivalued()
  },
  onlyEditing: function() {
    return this.get('property').onlyEditing()
  },
  showLabel: function() {
    return this.get('property').showLabel()
  },
  showValidationIssues: function() {
    return this.get('property').showValidationIssues()
  },
})
