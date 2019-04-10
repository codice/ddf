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

var template = require('./add-attribute.hbs')
var _ = require('underscore')
var Marionette = require('marionette')
var CustomElements = require('../../js/CustomElements.js')
var PropertyView = require('../property/property.view.js')
var Property = require('../property/property.js')
var properties = require('../../js/properties.js')
var metacardDefinitions = require('../singletons/metacard-definitions.js')

function determineMissingAttributes(selectionInterface) {
  var attributes = _.union.apply(
    this,
    selectionInterface.getSelectedResults().map(result => {
      return Object.keys(
        result
          .get('metacard')
          .get('properties')
          .toJSON()
      )
    })
  )
  var types = _.union.apply(
    this,
    selectionInterface.getSelectedResults().map(result => {
      return [result.get('metacardType')]
    })
  )
  var possibleAttributes = _.intersection.apply(
    this,
    types.map(type => {
      return Object.keys(metacardDefinitions.metacardDefinitions[type])
    })
  )
  var missingAttributes = _.difference(possibleAttributes, attributes)
  return metacardDefinitions
    .sortMetacardTypes(
      missingAttributes
        .map(attribute => metacardDefinitions.metacardTypes[attribute])
        .filter(definition => !definition.hidden && !definition.readOnly)
    )
    .map(definition => ({
      label: definition.alias || definition.id,
      value: definition.id,
    }))
}

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('add-attribute'),
  template: template,
  regions: {
    attributeSelector: '> .attribute-selector',
  },
  addAttribute: function() {
    this.model.set(
      'value',
      this.attributeSelector.currentView.model.get('value')
    )
  },
  onBeforeShow: function() {
    var missingAttributes = determineMissingAttributes(
      this.options.selectionInterface
    )
    this.attributeSelector.show(
      new PropertyView({
        model: new Property({
          enum: missingAttributes,
          id: 'Attributes To Add',
          value: [[]],
          showValidationIssues: false,
          enumFiltering: true,
          enumMulti: true,
        }),
      })
    )
    this.attributeSelector.currentView.turnOnEditing()
    this.listenTo(
      this.attributeSelector.currentView.model,
      'change:value',
      this.addAttribute
    )
  },
})
