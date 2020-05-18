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
const $ = require('jquery')
const _ = require('underscore')
const Marionette = require('marionette')
const CustomElements = require('../../../js/CustomElements.js')
const user = require('../../singletons/user-instance.js')
const properties = require('../../../js/properties.js')
const metacardDefinitions = require('../../singletons/metacard-definitions.js')

module.exports = Marionette.ItemView.extend({
  template(columns: any[]) {
    return (
      <React.Fragment>
        <div className="visibility-columns">
          {columns.map(column => {
            return (
              <div
                className={`column ${column.hidden ? 'is-hidden-column' : ''} ${
                  column.notCurrentlyAvailable ? 'is-unavailable-column' : ''
                }`}
                data-propertyid={column.id}
                data-propertytext={column.label ? column.label : column.id}
              >
                <span className="column-text">
                  {column.label ? column.label : column.id}
                </span>
              </div>
            )
          })}
        </div>
        <div className="visibility-footer">
          <button className="footer-cancel is-negative">
            <span className="fa fa-times" />
            <span>Cancel</span>
          </button>
          <button className="footer-save is-positive">
            <span className="fa fa-floppy-o" />
            <span>Save</span>
          </button>
        </div>
      </React.Fragment>
    )
  },
  tagName: 'intrigue-table-visibility',
  events: {
    'click .column': 'toggleVisibility',
    'click .footer-cancel': 'psuedoDestroy',
    'click .footer-save': 'handleSave',
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
  },
  serializeData() {
    const preferredHeader = user
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

    return preferredHeader.map(property => ({
      label: properties.attributeAliases[property],
      id: property,
      hidden: hiddenColumns.indexOf(property) >= 0,

      notCurrentlyAvailable:
        availableAttributes.indexOf(property) === -1 ||
        properties.isHidden(property) ||
        metacardDefinitions.isHiddenTypeExceptThumbnail(property),
    }))
  },
  toggleVisibility(e) {
    $(e.currentTarget).toggleClass('is-hidden-column')
  },
  onRender() {},
  psuedoDestroy() {
    if (this.options.destroy) {
      this.options.destroy()
    } else {
      this.destroy()
    }
  },
  handleSave() {
    const prefs = user.get('user').get('preferences')
    prefs.set(
      'columnHide',
      _.map(this.$el.find('.is-hidden-column'), element =>
        element.getAttribute('data-propertyid')
      )
    )
    prefs.savePreferences()
    this.psuedoDestroy()
  },
})
