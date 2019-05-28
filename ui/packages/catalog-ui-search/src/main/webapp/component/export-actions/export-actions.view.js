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

const Marionette = require('marionette')
const template = require('./export-actions.hbs')
const CustomElements = require('../../js/CustomElements.js')
const _ = require('lodash')

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('export-actions'),
  className: 'composed-menu',
  events: {
    'click > div': 'triggerAction',
  },
  triggerAction(e) {
    window.open(e.target.getAttribute('data-url'))
  },
  serializeData() {
    const exportActions = this.model.getExportActions()
    return _.sortBy(
      exportActions.map(action => ({
        url: action.get('url'),
        title: action.getExportType(),
      })),
      action => action.title.toLowerCase()
    )
  },
})
