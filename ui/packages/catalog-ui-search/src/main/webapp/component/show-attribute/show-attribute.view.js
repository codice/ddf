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
const template = require('./show-attribute.hbs')
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const user = require('../singletons/user-instance.js')
const PropertyView = require('../property/property.view.js')
const Property = require('../property/property.js')
const properties = require('../../js/properties.js')
const metacardDefinitions = require('../singletons/metacard-definitions.js')

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
}

function calculateAvailableAttributesFromActive(selectionInterface) {
  return selectionInterface
    .getActiveSearchResults()
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
}

function calculateSummaryAttributes() {
  const propertiesToShow = []
  const userPropertyArray = user.getSummaryShown()
  const propertiesArray =
    userPropertyArray.length > 0 ? userPropertyArray : properties.summaryShow
  return propertiesArray
}

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('show-attribute'),
  regions: {
    attributeSelector: '> .attribute-selector',
  },
  events: {
    'click > button': 'handleReset',
  },
  handleReset() {
    const prefs = user.get('user').get('preferences')
    prefs.set('inspector-summaryShown', [])
    prefs.savePreferences()
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
    this.onBeforeShow()
  },
  initialize(options) {
    if (!options.selectionInterface) {
      throw 'Selection interface has not been provided'
    }
    this.listenTo(
      this.options.selectionInterface,
      'reset:activeSearchResults add:activeSearchResults',
      this.onBeforeShow
    )
    this.listenTo(
      user.get('user').get('preferences'),
      'change:inspector-summaryShown',
      this.handleSummaryShown
    )
    this.handleSummaryShown()
  },
  handleSummaryShown() {
    const usersChoice = user
      .get('user')
      .get('preferences')
      .get('inspector-summaryShown')
    this.$el.toggleClass('has-custom-summary', usersChoice.length > 0)
  },
  onBeforeShow() {
    const attributes = calculateAvailableAttributesFromSelection(
      this.options.selectionInterface
    )
    const summaryAttributes = calculateSummaryAttributes()
    const totalAttributes = filterAndSort(
      _.union(attributes, summaryAttributes)
    )
    this.attributeSelector.show(
      new PropertyView({
        model: new Property({
          enum: totalAttributes.map(attr => {
            return {
              label: metacardDefinitions.getLabel(attr),
              value: attr,
            }
          }),
          id: 'Attributes To Show',
          value: [summaryAttributes],
          showValidationIssues: false,
          enumFiltering: true,
          enumMulti: true,
          onlyEditing: true,
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
      'inspector-summaryShown',
      this.attributeSelector.currentView.model.get('value')[0]
    )
    prefs.savePreferences()
  },
})
