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
  setValue(value) {
    this.set('value', value)
  },
  getValue() {
    return this.get('value')
  },
  setIsValid(isValid) {
    this.set('isValid', isValid)
  },
  isValid() {
    return this.get('isValid')
  },
  getCalculatedType() {
    return this.get('property').getCalculatedType()
  },
  getId() {
    return this.get('property').getId()
  },
  isReadOnly() {
    return this.get('property').isReadOnly()
  },
  isEditing() {
    return this.get('property').isEditing()
  },
  isMultivalued() {
    return this.get('property').isMultivalued()
  },
  onlyEditing() {
    return this.get('property').onlyEditing()
  },
  showLabel() {
    return this.get('property').showLabel()
  },
  showValidationIssues() {
    return this.get('property').showValidationIssues()
  },
})
