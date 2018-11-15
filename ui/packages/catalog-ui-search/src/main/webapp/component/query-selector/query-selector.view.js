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
/*global define, setTimeout*/
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const querySelectorTemplate = require('./query-selector.hbs')
const CustomElements = require('../../js/CustomElements.js')
const store = require('../../js/store.js')
const Query = require('../../js/model/Query.js')
const QueryItemCollectionView = require('../query-item/query-item.collection.view.js')

var namespace = CustomElements.getNamespace()

var QuerySelector = Marionette.LayoutView.extend({
  setDefaultModel: function() {
    this.model = store.getCurrentQueries()
  },
  template: querySelectorTemplate,
  tagName: CustomElements.register('query-selector'),
  modelEvents: {},
  events: function() {
    var eventObj = {
      'click .querySelector-add': 'addQuery',
      'click > .if-empty .quick-add': 'triggerQuery',
    }
    eventObj[
      'click .querySelector-list ' +
        CustomElements.getNamespace() +
        'query-item'
    ] = 'selectQuery'
    return eventObj
  },
  ui: {},
  regions: {
    queryCollection: '.querySelector-list',
  },
  onBeforeShow: function() {
    this.queryCollection.show(new QueryItemCollectionView())
    this.queryCollection.currentView.$el
      .addClass('is-list')
      .addClass('has-list-highlighting')
  },
  initialize: function(options) {
    if (options.model === undefined) {
      this.setDefaultModel()
    }
    this.handleUpdate()
    this.listenTo(this.model, 'add', this.handleUpdate)
    this.listenTo(this.model, 'remove', this.handleUpdate)
    this.listenTo(this.model, 'update', this.handleUpdate)
    this.listenTo(store.get('content'), 'change:query', this.handleQuerySelect)
  },
  addQuery: function() {
    if (this.model.canAddQuery()) {
      var newQuery = new Query.Model()
      store.setQueryByReference(newQuery)
    }
  },
  selectQuery: function(event) {
    var queryId = event.currentTarget.getAttribute('data-queryId')
    store.setQueryById(queryId)
  },
  handleQuerySelect: function() {
    var query = store.getQuery()
    this.$el.find(namespace + 'query-item').removeClass('is-selected')
    if (query) {
      this.$el
        .find(namespace + 'query-item[data-queryid="' + query.id + '"]')
        .addClass('is-selected')
    }
  },
  handleUpdate: function() {
    this.handleMaxQueries()
    this.handleEmptyQueries()
  },
  handleMaxQueries: function() {
    this.$el.toggleClass('can-addQuery', this.model.canAddQuery())
  },
  handleEmptyQueries: function() {
    this.$el.toggleClass('is-empty', this.model.isEmpty())
  },
  triggerQuery: function() {
    $('.content-adhoc')
      .mousedown()
      .click()
  },
})

module.exports = QuerySelector
