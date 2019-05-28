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
const CustomElements = require('../../js/CustomElements.js')
const template = require('./search-select.hbs')
const QuerySelectView = require('../query-select/query-select.view.js')
const store = require('../../js/store.js')
const Query = require('../../js/model/Query.js')
const $ = require('jquery')

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('search-select'),
  regions: {
    searchResults: '> .select-list',
  },
  className: 'composed-menu',
  events: {
    'click > button.select-add': 'addQuery',
    'click > .select-one .quick-add': 'triggerQuery',
  },
  initialize() {
    this.listenTo(store.getCurrentQueries(), 'add', this.handleUpdate)
    this.listenTo(store.getCurrentQueries(), 'remove', this.handleUpdate)
    this.listenTo(store.getCurrentQueries(), 'update', this.handleUpdate)
    this.handleUpdate()
    this.handleHideActions()
  },
  handleHideActions() {
    this.$el.toggleClass('hide-actions', this.options.hideActions === true)
  },
  onBeforeShow() {
    if (store.getCurrentWorkspace()) {
      this.setupSearchResults()
    }
  },
  onRender() {
    this.handleUpdate()
  },
  setupSearchResults() {
    this.searchResults.show(
      new QuerySelectView({
        model: this.model,
      })
    )
  },
  addQuery() {
    if (store.getCurrentWorkspace().canAddQuery()) {
      const newQuery = new Query.Model()
      store.setQueryByReference(newQuery)
      this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
    }
  },
  handleUpdate() {
    this.handleMaxQueries()
    this.handleEmptyQueries()
    this.handleOneQuery()
  },
  handleMaxQueries() {
    this.$el.toggleClass(
      'is-limited',
      !store.getCurrentWorkspace().canAddQuery()
    )
    this.$el
      .find('.select-add')
      .toggleClass('is-disabled', !store.getCurrentWorkspace().canAddQuery())
  },
  handleEmptyQueries() {
    this.$el.toggleClass('is-empty', store.getCurrentQueries().isEmpty())
  },
  handleOneQuery() {
    this.$el.toggleClass('has-one', store.getCurrentQueries().length === 1)
  },
  triggerQuery() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
    $('.content-adhoc')
      .mousedown()
      .click()
  },
})
