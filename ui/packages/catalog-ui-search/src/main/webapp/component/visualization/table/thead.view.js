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
const _ = require('underscore')
const $ = require('jquery')
const Marionette = require('marionette')
const CustomElements = require('../../../js/CustomElements.js')
const user = require('../../singletons/user-instance.js')
const properties = require('../../../js/properties.js')
const metacardDefinitions = require('../../singletons/metacard-definitions.js')
require('jquery-ui/ui/widgets/resizable')
let isResizing = false
const {
  ItemCheckboxAll,
} = require('../../selection-checkbox/item-checkbox-all')

module.exports = Marionette.LayoutView.extend({
  template({ headers }) {
    console.log(headers)
    return (
      <React.Fragment>
        <th className="checkbox-th">
          <ItemCheckboxAll
            selectionInterface={this.options.selectionInterface}
          />
        </th>
        {headers.map(header => {
          const { hidden, sortable, label, id } = header
          return (
            <th
              className={`${hidden ? 'is-hidden-column' : ''} ${
                sortable ? 'is-sortable' : ''
              }`}
              data-propertyid={`${id}`}
              data-propertytext={`${label ? `${label} ${id}` : `${id}`}`}
            >
              <span
                className="column-text"
                title={`${label ? `${label} ${id}` : `${id}`}`}
              >
                {`${label ? `${label} ${id}` : `${id}`}`}
              </span>
              <span className="fa fa-sort-asc"></span>
              <span className="fa fa-sort-desc"></span>
              <div className="resizer"></div>
            </th>
          )
        })}
      </React.Fragment>
    )
  },
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
      'change:columnHide',
      this.render
    )
    this.listenTo(
      user.get('user').get('preferences'),
      'change:columnOrder',
      this.render
    )
    this.updateSorting = _.debounce(this.updateSorting, 500)
  },
  onRender() {
    this.handleSorting()
    this.$el.find('.resizer').resizable({
      handles: 'e',
    })
  },
  updateSorting(e) {
    const attribute = e.currentTarget.getAttribute('data-propertyid')
    const $currentTarget = $(e.currentTarget)
    const direction = $currentTarget.hasClass('is-sorted-asc')
      ? 'descending'
      : 'ascending'
    const sort = [
      {
        attribute,
        direction,
      },
    ]
    const prefs = user.get('user').get('preferences')
    prefs.set('resultSort', sort)
    prefs.savePreferences()
  },
  handleSorting() {
    const resultSort = user
      .get('user')
      .get('preferences')
      .get('resultSort')
    this.$el.children('.is-sorted-asc').removeClass('is-sorted-asc')
    this.$el.children('.is-sorted-desc').removeClass('is-sorted-desc')
    if (resultSort) {
      resultSort.forEach(sort => {
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
      })
    }
  },
  serializeData() {
    const sortAttributes = _.filter(
      metacardDefinitions.sortedMetacardTypes,
      type => !metacardDefinitions.isHiddenTypeExceptThumbnail(type.id)
    ).map(type => type.id)
    const prefs = user.get('user').get('preferences')
    let preferredHeader = user
      .get('user')
      .get('preferences')
      .get('columnOrder')
    const hiddenColumns = user
      .get('user')
      .get('preferences')
      .get('columnHide')
    const availableAttributes = this.options.filteredAttributes.get(
      'filteredAttributes'
    )

    // tack on unknown attributes to end (sorted), then save
    preferredHeader = _.union(preferredHeader, availableAttributes)
    prefs.set('columnOrder', preferredHeader)
    prefs.savePreferences()

    return {
      headers: preferredHeader
        .filter(property => availableAttributes.indexOf(property) !== -1)
        .map(property => ({
          label: properties.attributeAliases[property],
          id: property,

          hidden:
            hiddenColumns.indexOf(property) >= 0 ||
            properties.isHidden(property) ||
            metacardDefinitions.isHiddenTypeExceptThumbnail(property),

          sortable: sortAttributes.indexOf(property) >= 0,
        })),
    }
  },
  updateColumnWidth(e) {
    $(e.currentTarget).css('width', $(e.target).width())
  },
  startResize(e) {
    isResizing = true
  },
  stopResize(e) {
    setTimeout(() => {
      isResizing = false
    }, 500)
  },
  checkIfResizing(e) {
    if (!isResizing) {
      this.updateSorting(e)
    }
  },
})
