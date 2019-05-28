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
const template = require('./attributes-rearrange.hbs')
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const user = require('../singletons/user-instance.js')
const properties = require('../../js/properties.js')
const Sortable = require('sortablejs')
const metacardDefinitions = require('../singletons/metacard-definitions.js')

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

module.exports = Marionette.ItemView.extend({
  template,
  tagName: CustomElements.register('attributes-rearrange'),
  initialize(options) {
    if (!options.selectionInterface) {
      throw 'Selection interface has not been provided'
    }
    this.listenTo(
      this.options.selectionInterface,
      'reset:activeSearchResults add:activeSearchResults',
      this.render
    )
    this.listenTo(
      user.get('user').get('preferences'),
      'change:inspector-summaryShown',
      this.render
    )
    this.listenTo(
      user.get('user').get('preferences'),
      'change:inspector-detailsHidden',
      this.render
    )
  },
  getShown() {
    if (this.options.summary) {
      const usersChoice = user
        .get('user')
        .get('preferences')
        .get('inspector-summaryShown')
      if (usersChoice.length > 0) {
        return usersChoice
      } else {
        return properties.summaryShow
      }
    } else {
      return calculateAvailableAttributesFromSelection(
        this.options.selectionInterface
      )
    }
  },
  getHidden() {
    if (this.options.summary) {
      const usersChoice = user
        .get('user')
        .get('preferences')
        .get('inspector-summaryShown')
      if (usersChoice.length > 0) {
        return calculateAvailableAttributesFromSelection(
          this.options.selectionInterface
        ).filter(attr => usersChoice.indexOf(attr) === -1)
      } else {
        return calculateAvailableAttributesFromSelection(
          this.options.selectionInterface
        ).filter(attr => properties.summaryShow.indexOf(attr) === -1)
      }
    } else {
      return user
        .get('user')
        .get('preferences')
        .get('inspector-detailsHidden')
    }
  },
  getPreferredOrder() {
    if (this.options.summary) {
      const usersShown = user
        .get('user')
        .get('preferences')
        .get('inspector-summaryShown')
      const usersOrder = user
        .get('user')
        .get('preferences')
        .get('inspector-summaryOrder')
      if (usersOrder.length > 0) {
        return usersOrder
      } else {
        return properties.summaryShow
      }
    } else {
      return user
        .get('user')
        .get('preferences')
        .get('inspector-detailsOrder')
    }
  },
  getNewAttributes() {
    if (this.options.summary) {
      const usersShown = user
        .get('user')
        .get('preferences')
        .get('inspector-summaryShown')
      const usersOrder = user
        .get('user')
        .get('preferences')
        .get('inspector-summaryOrder')
      if (usersShown.length > 0 || usersOrder.length > 0) {
        return usersShown.filter(attr => usersOrder.indexOf(attr) === -1)
      } else {
        return []
      }
    } else {
      const detailsOrder = user
        .get('user')
        .get('preferences')
        .get('inspector-detailsOrder')
      return calculateAvailableAttributesFromSelection(
        this.options.selectionInterface
      ).filter(attr => detailsOrder.indexOf(attr) === -1)
    }
  },
  serializeData() {
    const preferredHeader = this.getPreferredOrder()
    const newAttributes = this.getNewAttributes()
    newAttributes.sort((a, b) => metacardDefinitions.attributeComparator(a, b))
    const hidden = this.getHidden()
    const availableAttributes = calculateAvailableAttributesFromSelection(
      this.options.selectionInterface
    )

    return _.union(preferredHeader, newAttributes).map(property => ({
      label: properties.attributeAliases[property],
      id: property,
      hidden: hidden.indexOf(property) >= 0,

      notCurrentlyAvailable:
        availableAttributes.indexOf(property) === -1 ||
        properties.isHidden(property) ||
        metacardDefinitions.isHiddenTypeExceptThumbnail(property),
    }))
  },
  onRender() {
    Sortable.create(this.el, {
      onEnd: () => {
        this.handleSave()
      },
    })
  },
  handleSave() {
    const prefs = user.get('user').get('preferences')
    const key = this.options.summary
      ? 'inspector-summaryOrder'
      : 'inspector-detailsOrder'
    prefs.set(
      key,
      _.map(this.$el.find('.column'), element =>
        element.getAttribute('data-propertyid')
      )
    )
    prefs.savePreferences()
  },
})
