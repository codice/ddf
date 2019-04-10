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

var wreqr = require('../../js/wreqr.js')
var _ = require('underscore')
var template = require('./attributes-rearrange.hbs')
var Marionette = require('marionette')
var CustomElements = require('../../js/CustomElements.js')
var Common = require('../../js/Common.js')
var user = require('../singletons/user-instance.js')
var properties = require('../../js/properties.js')
var Sortable = require('sortablejs')
var metacardDefinitions = require('../singletons/metacard-definitions.js')

function calculateAvailableAttributesFromSelection(selectionInterface) {
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
  return selectionInterface
    .getSelectedResults()
    .reduce(function(currentAvailable, result) {
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
    .filter(function(property) {
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
  template: template,
  tagName: CustomElements.register('attributes-rearrange'),
  initialize: function(options) {
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
  getShown: function() {
    if (this.options.summary) {
      var usersChoice = user
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
  getHidden: function() {
    if (this.options.summary) {
      var usersChoice = user
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
  getPreferredOrder: function() {
    if (this.options.summary) {
      var usersShown = user
        .get('user')
        .get('preferences')
        .get('inspector-summaryShown')
      var usersOrder = user
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
  getNewAttributes: function() {
    if (this.options.summary) {
      var usersShown = user
        .get('user')
        .get('preferences')
        .get('inspector-summaryShown')
      var usersOrder = user
        .get('user')
        .get('preferences')
        .get('inspector-summaryOrder')
      if (usersShown.length > 0 || usersOrder.length > 0) {
        return usersShown.filter(function(attr) {
          return usersOrder.indexOf(attr) === -1
        })
      } else {
        return []
      }
    } else {
      var detailsOrder = user
        .get('user')
        .get('preferences')
        .get('inspector-detailsOrder')
      return calculateAvailableAttributesFromSelection(
        this.options.selectionInterface
      ).filter(function(attr) {
        return detailsOrder.indexOf(attr) === -1
      })
    }
  },
  serializeData: function() {
    var preferredHeader = this.getPreferredOrder()
    var newAttributes = this.getNewAttributes()
    newAttributes.sort(function(a, b) {
      return metacardDefinitions.attributeComparator(a, b)
    })
    var hidden = this.getHidden()
    var availableAttributes = calculateAvailableAttributesFromSelection(
      this.options.selectionInterface
    )

    return _.union(preferredHeader, newAttributes).map(function(property) {
      return {
        label: properties.attributeAliases[property],
        id: property,
        hidden: hidden.indexOf(property) >= 0,
        notCurrentlyAvailable:
          availableAttributes.indexOf(property) === -1 ||
          properties.isHidden(property) ||
          metacardDefinitions.isHiddenTypeExceptThumbnail(property),
      }
    })
  },
  onRender: function() {
    Sortable.create(this.el, {
      onEnd: () => {
        this.handleSave()
      },
    })
  },
  handleSave: function() {
    var prefs = user.get('user').get('preferences')
    var key = this.options.summary
      ? 'inspector-summaryOrder'
      : 'inspector-detailsOrder'
    prefs.set(
      key,
      _.map(this.$el.find('.column'), function(element) {
        return element.getAttribute('data-propertyid')
      })
    )
    prefs.savePreferences()
  },
})
