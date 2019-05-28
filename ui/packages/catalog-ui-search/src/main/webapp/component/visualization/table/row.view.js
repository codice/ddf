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

const template = require('./row.hbs')
const Marionette = require('marionette')
const CustomElements = require('../../../js/CustomElements.js')
const metacardDefinitions = require('../../singletons/metacard-definitions.js')
const user = require('../../singletons/user-instance.js')
const properties = require('../../../js/properties.js')
const HoverPreviewDropdown = require('../../dropdown/hover-preview/dropdown.hover-preview.view.js')
const DropdownModel = require('../../dropdown/dropdown.js')
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
  attributes() {
    return {
      'data-resultid': this.model.id,
    }
  },
  template,
  initialize(options) {
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
  handleSelectionChange() {
    const selectedResults = this.options.selectionInterface.getSelectedResults()
    const isSelected = selectedResults.get(this.model.id)
    this.$el.toggleClass('is-selected', Boolean(isSelected))
  },
  onRender() {
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
  handleResultThumbnail() {
    const hiddenColumns = user
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
  checkIfDownloadable() {
    this.$el.toggleClass(
      'is-downloadable',
      this.model
        .get('metacard')
        .get('properties')
        .get('resource-download-url') !== undefined
    )
  },
  checkIfLinks() {
    this.$el.toggleClass(
      'is-links',
      this.model
        .get('metacard')
        .get('properties')
        .get('associations.external') !== undefined
    )
  },
  triggerDownload() {
    window.open(
      this.model
        .get('metacard')
        .get('properties')
        .get('resource-download-url')
    )
  },
  serializeData() {
    const prefs = user.get('user').get('preferences')
    const preferredHeader = user
      .get('user')
      .get('preferences')
      .get('columnOrder')
    const availableAttributes = this.options.selectionInterface.getActiveSearchResultsAttributes()
    const result = this.model.toJSON()
    return {
      id: result.id,
      properties: preferredHeader
        .filter(property => availableAttributes.indexOf(property) !== -1)
        .map(property => {
          let value = result.metacard.properties[property]
          if (value === undefined) {
            value = ''
          }
          if (value.constructor !== Array) {
            value = [value]
          }
          let className = 'is-text'
          if (value && metacardDefinitions.metacardTypes[property]) {
            switch (metacardDefinitions.metacardTypes[property].type) {
              case 'DATE':
                value = value.map(
                  val =>
                    val !== undefined && val !== ''
                      ? user.getUserReadableDateTime(val)
                      : ''
                )
                break
              default:
                break
            }
          }
          if (property === 'thumbnail') {
            className = 'is-thumbnail'
          }
          return {
            property,
            value,
            class: className,
            hidden: this.isHidden(property),
          }
        }),
    }
  },
  isHidden(property) {
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
