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
const queriesTemplate = require('./queries.hbs')
const CustomElements = require('../../js/CustomElements.js')
const QueryTabsView = require('../tabs/query/tabs-query.view.js')
const QuerySelectorView = require('../query-selector/query-selector.view.js')
const store = require('../../js/store.js')

const Queries = Marionette.LayoutView.extend({
  setDefaultModel() {
    this.model = store.getCurrentWorkspace()
  },
  template: queriesTemplate,
  tagName: CustomElements.register('queries'),
  events: {
    'click .workspaces-details': 'shiftRight',
    'click .workspaces-selector': 'shiftLeft',
  },
  ui: {},
  regions: {
    queriesDetails: '.queries-details',
    queriesSelector: '.queries-selector',
  },
  initialize(options) {
    if (options.model === undefined) {
      this.setDefaultModel()
    }
    this.listenTo(store.get('content'), 'change:query', this.changeQuery)
  },
  onRender() {
    this.queriesSelector.show(new QuerySelectorView())
    this.changeQuery()
  },
  changeQuery() {
    const queryRef = store.getQuery()
    if (queryRef === undefined) {
      this.queriesDetails.empty()
    } else {
      this.shiftRight()
      this.queriesDetails.show(new QueryTabsView())
    }
  },
  shiftLeft(event) {
    this.$el.addClass('shifted-left')
    this.$el.removeClass('shifted-right')
  },
  shiftRight() {
    this.$el.addClass('shifted-right')
    this.$el.removeClass('shifted-left')
  },
})

module.exports = Queries
