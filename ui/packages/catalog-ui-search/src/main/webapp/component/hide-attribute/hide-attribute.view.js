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

const template = require('./hide-attribute.hbs')
const _ = require('underscore')
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const PropertyView = require('../property/property.view.js')
const Property = require('../property/property.js')
const metacardDefinitions = require('../singletons/metacard-definitions.js')
const user = require('../singletons/user-instance.js')

function filterAndSort(attributes) {
  return attributes
    .filter(property => {
      if (metacardDefinitions.metacardTypes[property]) {
        return !metacardDefinitions.metacardTypes[property].hidden
      } else {
        announcement.announce({
          title: 'Missing Attribute Definition',
          message:
            'Could not find information for ' +
            property +
            ' in definitions.  If this problem persists, contact your Administrator.',
          type: 'warn',
        })
        return false
      }
    })
    .sort((a, b) => metacardDefinitions.attributeComparator(a, b))
}

function calculateAvailableAttributesFromSelection(selectionInterface) {
  const types = _.union.apply(
    this,
    selectionInterface.getSelectedResults().map(result => {
      return [result.get('metacardType')]
    })
  )
  const possibleAttributes = _.intersection.apply(
    this,
    types.map(type => {
      return Object.keys(metacardDefinitions.metacardDefinitions[type])
    })
  )
  return selectionInterface
    .getSelectedResults()
    .reduce((currentAvailable, result) => {
      currentAvailable = _.union(
        currentAvailable,
        Object.keys(
          result
            .get('metacard')
            .get('properties')
            .toJSON()
        )
      )
      return currentAvailable
    }, [])
    .filter(attribute => possibleAttributes.indexOf(attribute) >= 0)
}

function calculateDetailsAttributes() {
  const userPropertyArray = user
    .get('user')
    .get('preferences')
    .get('inspector-detailsHidden')
  return userPropertyArray
}

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('hide-attribute'),
  template,
  regions: {
    attributeSelector: '> .attribute-selector',
  },
  onBeforeShow() {
    const attributes = calculateAvailableAttributesFromSelection(
      this.options.selectionInterface
    )
    const detailsAttributes = calculateDetailsAttributes()
    const totalAttributes = filterAndSort(
      _.union(attributes, detailsAttributes)
    )
    const detailsHidden = user
      .get('user')
      .get('preferences')
      .get('inspector-detailsHidden')
    this.attributeSelector.show(
      new PropertyView({
        model: new Property({
          enum: totalAttributes.map(attr => {
            return {
              label: metacardDefinitions.getLabel(attr),
              value: attr,
            }
          }),
          id: 'Attributes To Hide',
          value: [detailsHidden],
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
      this.handleSave
    )
  },
  handleSave() {
    const prefs = user.get('user').get('preferences')
    prefs.set(
      'inspector-detailsHidden',
      this.attributeSelector.currentView.model.get('value')[0]
    )
    prefs.savePreferences()
  },
})
