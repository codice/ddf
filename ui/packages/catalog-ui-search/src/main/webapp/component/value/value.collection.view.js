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

const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const ValueView = require('./value.view')
const ValueCollection = require('./value.collection')
const CustomElements = require('../../js/CustomElements.js')
const moment = require('moment')

module.exports = Marionette.CollectionView.extend(
  {
    childView: ValueView,
    tagName: CustomElements.register('value-collection'),
    collectionEvents: {
      'change:value': 'updateProperty',
      remove: 'updateProperty',
      add: 'updateProperty',
    },
    initialize: function() {
      this.updateProperty()
    },
    getValue: function() {
      return this.collection.map(function(valueModel) {
        return valueModel.getValue()
      })
    },
    updateProperty: function() {
      this.model.setValue(this.getValue())
    },
    addNewValue: function(propertyModel) {
      this.collection.add({
        value: propertyModel.getDefaultValue(),
        property: propertyModel,
      })
      this.children.last().focus()
    },
  },
  {
    generateValueCollectionView: function(propertyModel) {
      var valueCollection = new ValueCollection()
      if (propertyModel.get('value').length > 0) {
        valueCollection.add(
          propertyModel.get('value').map(function(value) {
            return {
              value: value,
              property: propertyModel,
            }
          })
        )
      } else if (!propertyModel.get('multivalued')) {
        valueCollection.add({
          value: null,
          property: propertyModel,
        })
      }
      return new this({
        collection: valueCollection,
        model: propertyModel,
      })
    },
  }
)
