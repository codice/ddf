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

var wreqr = require('../../../js/wreqr.js')
var $ = require('jquery')
var _ = require('underscore')
var template = require('./table-visibility.hbs')
var Marionette = require('marionette')
var CustomElements = require('../../../js/CustomElements.js')
var Common = require('../../../js/Common.js')
var user = require('../../singletons/user-instance.js')
var properties = require('../../../js/properties.js')
var metacardDefinitions = require('../../singletons/metacard-definitions.js')

module.exports = Marionette.ItemView.extend({
  template: template,
  tagName: CustomElements.register('table-visibility'),
  events: {
    'click .column': 'toggleVisibility',
    'click .footer-cancel': 'destroy',
    'click .footer-save': 'handleSave',
  },
  initialize: function(options) {
    if (!options.selectionInterface) {
      throw 'Selection interface has not been provided'
    }
    this.listenTo(
      this.options.selectionInterface,
      'reset:activeSearchResults add:activeSearchResults',
      this.render
    )
  },
  serializeData: function() {
    var prefs = user.get('user').get('preferences')
    var results = this.options.selectionInterface
      .getActiveSearchResults()
      .toJSON()
    var preferredHeader = user
      .get('user')
      .get('preferences')
      .get('columnOrder')
    var hiddenColumns = user
      .get('user')
      .get('preferences')
      .get('columnHide')
    var availableAttributes = this.options.selectionInterface.getActiveSearchResultsAttributes()

    return preferredHeader.map(function(property) {
      return {
        label: properties.attributeAliases[property],
        id: property,
        hidden: hiddenColumns.indexOf(property) >= 0,
        notCurrentlyAvailable:
          availableAttributes.indexOf(property) === -1 ||
          properties.isHidden(property) ||
          metacardDefinitions.isHiddenTypeExceptThumbnail(property),
      }
    })
  },
  toggleVisibility: function(e) {
    $(e.currentTarget).toggleClass('is-hidden-column')
  },
  onRender: function() {},
  handleSave: function() {
    var prefs = user.get('user').get('preferences')
    prefs.set(
      'columnHide',
      _.map(this.$el.find('.is-hidden-column'), function(element) {
        return element.getAttribute('data-propertyid')
      })
    )
    prefs.savePreferences()
    this.destroy()
  },
})
