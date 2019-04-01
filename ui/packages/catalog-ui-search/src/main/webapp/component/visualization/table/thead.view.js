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
/*global require, setTimeout*/
var _ = require('underscore')
var $ = require('jquery')
var template = require('./thead.hbs')
var Marionette = require('marionette')
var CustomElements = require('../../../js/CustomElements.js')
var user = require('../../singletons/user-instance.js')
var properties = require('../../../js/properties.js')
var metacardDefinitions = require('../../singletons/metacard-definitions.js')
require('jquery-ui/ui/widgets/resizable')
var isResizing = false
const CheckboxView = require('../../selection-checkbox/selection-checkbox.view.js')

module.exports = Marionette.LayoutView.extend({
  template: template,
  className: 'is-thead',
  tagName: CustomElements.register('result-thead'),
  events: {
    'click th.is-sortable': 'checkIfResizing',
    'resize th': 'updateColumnWidth',
    'resizestart th': 'startResize',
    'resizestop th': 'stopResize',
  },
  regions: {
    checkboxContainer: '.checkbox-container',
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
    this.listenTo(
      user.get('user').get('preferences'),
      'change:columnHide',
      this.render
    )
    this.listenTo(
      user.get('user').get('preferences'),
      'change:columnOrder',
      this.render
    )
    this.listenTo(
      this.options.selectionInterface.getSelectedResults(),
      'update add remove reset',
      this.updateCheckbox
    )
    this.updateSorting = _.debounce(this.updateSorting, 500)
  },
  onRender: function() {
    this.handleSorting()
    this.$el.find('.resizer').resizable({
      handles: 'e',
    })
    this.showCheckbox()
  },
  showCheckbox: function() {
    this.checkboxContainer.show(
      new CheckboxView({
        isSelected: this.allSelected(),
        onClick: this.toggleCurrentResults.bind(this),
      })
    )
  },
  updateCheckbox: function() {
    if (this.checkboxContainer.currentView) {
      this.checkboxContainer.currentView.setCheck(this.allSelected())
    }
  },
  updateSorting: function(e) {
    var attribute = e.currentTarget.getAttribute('data-propertyid')
    var $currentTarget = $(e.currentTarget)
    var direction = $currentTarget.hasClass('is-sorted-asc')
      ? 'descending'
      : 'ascending'
    var sort = [
      {
        attribute: attribute,
        direction: direction,
      },
    ]
    var prefs = user.get('user').get('preferences')
    prefs.set('resultSort', sort)
    prefs.savePreferences()
  },
  handleSorting: function() {
    var resultSort = user
      .get('user')
      .get('preferences')
      .get('resultSort')
    this.$el.children('.is-sorted-asc').removeClass('is-sorted-asc')
    this.$el.children('.is-sorted-desc').removeClass('is-sorted-desc')
    if (resultSort) {
      resultSort.forEach(
        function(sort) {
          switch (sort.direction) {
            case 'ascending':
              this.$el
                .children('[data-propertyid="' + sort.attribute + '"]')
                .addClass('is-sorted-asc')
              break
            case 'descending':
              this.$el
                .children('[data-propertyid="' + sort.attribute + '"]')
                .addClass('is-sorted-desc')
              break
            default:
              break
          }
        }.bind(this)
      )
    }
  },
  serializeData: function() {
    var sortAttributes = _.filter(
      metacardDefinitions.sortedMetacardTypes,
      function(type) {
        return !metacardDefinitions.isHiddenTypeExceptThumbnail(type.id)
      }
    ).map(function(type) {
      return type.id
    })
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

    // tack on unknown attributes to end (sorted), then save
    preferredHeader = _.union(preferredHeader, availableAttributes)
    prefs.set('columnOrder', preferredHeader)
    prefs.savePreferences()

    return preferredHeader
      .filter(function(property) {
        return availableAttributes.indexOf(property) !== -1
      })
      .map(function(property) {
        return {
          label: properties.attributeAliases[property],
          id: property,
          hidden:
            hiddenColumns.indexOf(property) >= 0 ||
            properties.isHidden(property) ||
            metacardDefinitions.isHiddenTypeExceptThumbnail(property),
          sortable: sortAttributes.indexOf(property) >= 0,
        }
      })
  },
  updateColumnWidth: function(e) {
    $(e.currentTarget).css('width', $(e.target).width())
  },
  startResize: function(e) {
    isResizing = true
  },
  stopResize: function(e) {
    setTimeout(function() {
      isResizing = false
    }, 500)
  },
  checkIfResizing: function(e) {
    if (!isResizing) {
      this.updateSorting(e)
    }
  },
  allSelected: function() {
    const currentResultsLength = this.options.selectionInterface.getActiveSearchResults()
      .length
    return (
      currentResultsLength > 0 &&
      currentResultsLength ===
        this.options.selectionInterface.getSelectedResults().length
    )
  },
  toggleCurrentResults: function() {
    if (this.allSelected()) {
      this.options.selectionInterface.clearSelectedResults()
    } else {
      const currentResults = this.options.selectionInterface.getActiveSearchResults()
      this.options.selectionInterface.addSelectedResult(currentResults.models)
    }
  },
})
