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
import * as React from 'react'
import { ItemCheckbox } from '../../selection-checkbox/item-checkbox'

const Marionette = require('marionette')
const CustomElements = require('../../../js/CustomElements.js')
const metacardDefinitions = require('../../singletons/metacard-definitions.js')
const user = require('../../singletons/user-instance.js')
const properties = require('../../../js/properties.js')
const HoverPreviewDropdown = require('../../dropdown/hover-preview/dropdown.hover-preview.view.js')
const DropdownModel = require('../../dropdown/dropdown.js')
const HandleBarsHelpers = require('../../../js/HandlebarsHelpers')

module.exports = Marionette.LayoutView.extend({
  className: 'is-tr',
  tagName: CustomElements.register('result-row'),
  events: {
    'click .result-download': 'triggerDownload',
  },
  regions: {
    resultThumbnail: '.is-thumbnail',
  },
  attributes() {
    return {
      'data-resultid': this.model.id,
    }
  },
  template({ properties, id }) {
    return (
      <React.Fragment>
        <td>
          <ItemCheckbox
            selectionInterface={this.options.selectionInterface}
            model={this.model}
          />
        </td>
        {properties.filter(property => !property.hidden).map(property => {
          const alias = HandleBarsHelpers.getAlias(property.property)
          return (
            <td
              data-property={`${property.property}`}
              className={`${property.class} ${
                property.hidden ? 'is-hidden-column' : ''
              }`}
              data-value={`${property.value}`}
            >
              <div>
                {property.value.map(value => {
                  console.log(value)
                  return (
                    <span data-value={`${value}`} title={`${alias}: ${value}`}>
                      {value.toString().substring(0, 4) === 'http' ? (
                        <a
                          href={`${value}`}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          {HandleBarsHelpers.getAlias(property.property)}
                        </a>
                      ) : (
                        `${value}`
                      )}
                    </span>
                  )
                })}
              </div>
              <div className="for-bold">
                {property.value.map(value => (
                  <span data-value={`${value}`}>
                    {value.toString().substring(0, 4) === 'http' ? (
                      <a
                        href={`${value}`}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        {HandleBarsHelpers.getAlias(property.property)}
                      </a>
                    ) : (
                      `${value}`
                    )}
                    :{value}
                  </span>
                ))}
              </div>
            </td>
          )
        })}
      </React.Fragment>
    )
  },
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
    this.listenTo(
      this.options.filteredAttributes,
      'change:filteredAttributes',
      this.render
    )
    this.handleSelectionChange()
  },
  handleSelectionChange() {
    const selectedResults = this.options.selectionInterface.getSelectedResults()
    const isSelected = selectedResults.get(this.model.id)
    this.$el.toggleClass('is-selected', Boolean(isSelected))
  },
  onRender() {
    this.checkIfLinks()
    this.$el.attr(this.attributes())
    this.handleResultThumbnail()
  },
  handleResultThumbnail() {
    if (
      this.model
        .get('metacard')
        .get('properties')
        .get('thumbnail') &&
      !this.isHidden('thumbnail')
    ) {
      if (this.resultThumbnail.$el) {
        this.resultThumbnail.show(
          new HoverPreviewDropdown({
            model: new DropdownModel(),
            modelForComponent: this.model,
          })
        )
      }
    }
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
    const preferredHeader = user
      .get('user')
      .get('preferences')
      .get('columnOrder')
    const availableAttributes = this.options.filteredAttributes.get(
      'filteredAttributes'
    )
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
              case 'LONG':
              case 'DOUBLE':
              case 'FLOAT':
              case 'INTEGER':
              case 'SHORT':
                value = value.map(
                  val => (val !== undefined && val !== '' ? Number(val) : '')
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
