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
const template = require('./result-filter.hbs')
const CustomElements = require('../../js/CustomElements.js')
const user = require('../singletons/user-instance.js')
const FilterBuilderView = require('../filter-builder/filter-builder.view.js')
const cql = require('../../js/cql.js')

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('result-filter'),
  modelEvents: {
    change: 'render',
  },
  events: {
    'click > .editor-footer .footer-remove': 'removeFilter',
    'click > .editor-footer .footer-save': 'saveFilter',
  },
  ui: {},
  regions: {
    editorProperties: '.editor-properties',
  },
  initialize() {},
  getResultFilter() {
    return user
      .get('user')
      .get('preferences')
      .get('resultFilter')
  },
  onRender() {
    const resultFilter = this.getResultFilter()
    let filter
    if (resultFilter) {
      filter = cql.simplify(cql.read(resultFilter))
    } else {
      filter = {
        property: 'anyText',
        value: '',
        type: 'ILIKE',
      }
    }
    this.editorProperties.show(
      new FilterBuilderView({
        filter,
        isResultFilter: true,
      })
    )
    this.editorProperties.currentView.turnOnEditing()
    this.editorProperties.currentView.turnOffNesting()
    this.handleFilter()
  },
  getFilter() {
    return this.editorProperties.currentView.transformToCql()
  },
  removeFilter() {
    user
      .get('user')
      .get('preferences')
      .set('resultFilter', undefined)
    user
      .get('user')
      .get('preferences')
      .savePreferences()
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
  saveFilter() {
    user
      .get('user')
      .get('preferences')
      .set('resultFilter', this.getFilter())
    user
      .get('user')
      .get('preferences')
      .savePreferences()
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
  handleFilter() {
    const resultFilter = this.getResultFilter()
    this.$el.toggleClass('has-filter', Boolean(resultFilter))
  },
})
