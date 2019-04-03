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
/*global require*/
var template = require('./row.hbs')
var Marionette = require('marionette')
var CustomElements = require('../../../js/CustomElements.js')
var metacardDefinitions = require('../../singletons/metacard-definitions.js')
var user = require('../../singletons/user-instance.js')
var properties = require('../../../js/properties.js')
var HoverPreviewDropdown = require('../../dropdown/hover-preview/dropdown.hover-preview.view.js')
var DropdownModel = require('../../dropdown/dropdown.js')
const {
  SelectItemToggle,
} = require('../../selection-checkbox/selection-checkbox.view.js')

module.exports = Marionette.LayoutView.extend({
  className: 'is-tr',
  tagName: CustomElements.register('result-row'),
  events: {
    'click .result-download': 'triggerDownload',
  },
  regions: {
    resultThumbnail: '.is-thumbnail',
    checkboxContainer: '.checkbox-container',
  },
  attributes: function() {
    return {
      'data-resultid': this.model.id,
    }
  },
  template: template,
  initialize: function(options) {
    if (!options.selectionInterface) {
      throw 'Selection interface has not been provided'
    }
    this.listenTo(
      this.model,
      'change:metacard>properties change:metacard',
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
      this.handleSelectionChange
    )
    this.handleSelectionChange()
  },
  handleSelectionChange: function() {
    var selectedResults = this.options.selectionInterface.getSelectedResults()
    var isSelected = selectedResults.get(this.model.id)
    this.$el.toggleClass('is-selected', Boolean(isSelected))
  },
  onRender: function() {
    this.checkIfDownloadable()
    this.checkIfLinks()
    this.$el.attr(this.attributes())
    this.handleResultThumbnail()
    this.showCheckboxSelector()
  },
  showCheckboxSelector() {
    this.checkboxContainer.show(
      new SelectItemToggle({
        model: this.model,
        selectionInterface: this.options.selectionInterface,
      })
    )
  },
  handleResultThumbnail: function() {
    var hiddenColumns = user
      .get('user')
      .get('preferences')
      .get('columnHide')
    if (
      this.model
        .get('metacard')
        .get('properties')
        .get('thumbnail') &&
      !this.isHidden('thumbnail')
    ) {
      this.resultThumbnail.show(
        new HoverPreviewDropdown({
          model: new DropdownModel(),
          modelForComponent: this.model,
        })
      )
    }
  },
  checkIfDownloadable: function() {
    this.$el.toggleClass(
      'is-downloadable',
      this.model
        .get('metacard')
        .get('properties')
        .get('resource-download-url') !== undefined
    )
  },
  checkIfLinks: function() {
    this.$el.toggleClass(
      'is-links',
      this.model
        .get('metacard')
        .get('properties')
        .get('associations.external') !== undefined
    )
  },
  triggerDownload: function() {
    window.open(
      this.model
        .get('metacard')
        .get('properties')
        .get('resource-download-url')
    )
  },
  serializeData: function() {
    var prefs = user.get('user').get('preferences')
    var preferredHeader = user
      .get('user')
      .get('preferences')
      .get('columnOrder')
    var availableAttributes = this.options.selectionInterface.getActiveSearchResultsAttributes()
    var result = this.model.toJSON()
    return {
      id: result.id,
      properties: preferredHeader
        .filter(function(property) {
          return availableAttributes.indexOf(property) !== -1
        })
        .map(property => {
          var value = result.metacard.properties[property]
          if (value === undefined) {
            value = ''
          }
          if (value.constructor !== Array) {
            value = [value]
          }
          var className = 'is-text'
          if (value && metacardDefinitions.metacardTypes[property]) {
            switch (metacardDefinitions.metacardTypes[property].type) {
              case 'DATE':
                value = value.map(function(val) {
                  return val !== undefined && val !== ''
                    ? user.getUserReadableDateTime(val)
                    : ''
                })
                break
              default:
                break
            }
          }
          if (property === 'thumbnail') {
            className = 'is-thumbnail'
          }
          return {
            property: property,
            value: value,
            class: className,
            hidden: this.isHidden(property),
          }
        }),
    }
  },
  isHidden: function(property) {
    const hiddenColumns = user
      .get('user')
      .get('preferences')
      .get('columnHide')

    return (
      hiddenColumns.indexOf(property) >= 0 ||
      properties.isHidden(property) ||
      metacardDefinitions.isHiddenTypeExceptThumbnail(property)
    )
  },
})
