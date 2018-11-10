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
/*global define, require, module*/
var Marionette = require('marionette')
var _ = require('underscore')
var _merge = require('lodash/merge')
var $ = require('jquery')
var template = require('./list-item.hbs')
var CustomElements = require('../../js/CustomElements.js')
require('../../behaviors/button.behavior.js')
require('../../behaviors/dropdown.behavior.js')
var ListEditorView = require('../list-editor/list-editor.view.js')
var QueryFeedView = require('../query-feed/query-feed.view.js')
var ListInteractionsView = require('../list-interactions/list-interactions.view.js')
var lightboxInstance = require('../lightbox/lightbox.view.instance.js')
var ListAddTabsView = require('../tabs/list-add/tabs-list-add.view.js')

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('list-item'),
  template: template,
  attributes: function() {
    return {
      'data-listid': this.model.id,
    }
  },
  regions: {
    queryFeed: '.details-feed',
    listAdd: '.list-add',
  },
  events: {
    'click .list-run': 'triggerRun',
    'click .list-refresh': 'triggerRun',
    'click .list-stop': 'triggerStop',
    'click .list-delete': 'triggerDelete',
    'click .list-add': 'triggerAdd',
  },
  behaviors() {
    return {
      button: {},
      dropdown: {
        dropdowns: [
          {
            selector: '.list-actions',
            view: ListInteractionsView.extend({
              behaviors: {
                navigation: {},
              },
            }),
            viewOptions: {
              model: this.options.model,
            },
          },
          {
            selector: '.list-edit',
            view: ListEditorView,
            viewOptions: {
              model: this.options.model,
            },
          },
        ],
      },
    }
  },
  initialize: function() {
    if (this.model.get('query').has('result')) {
      this.startListeningToStatus()
    } else {
      this.listenTo(this.model.get('query'), 'change:result', this.resultAdded)
    }
    this.listenTo(this.model, 'change:query>isOutdated', this.handleOutOfDate)
    this.listenTo(this.model, 'change:list.bookmarks', this.handleEmptyList)
    this.handleEmptyList()
    this.handleOutOfDate()
  },
  handleOutOfDate: function() {
    this.$el.toggleClass('is-out-of-date', this.model.get('query>isOutdated'))
  },
  handleEmptyList: function() {
    this.$el.toggleClass('is-empty', this.model.isEmpty())
  },
  onRender: function() {
    this.setupFeed()
  },
  setupFeed: function() {
    this.queryFeed.show(
      new QueryFeedView({
        model: this.model.get('query'),
      })
    )
  },
  resultAdded: function(model) {
    if (
      this.model.get('query').has('result') &&
      _.isUndefined(this.model.get('query').previous('result'))
    ) {
      this.startListeningToStatus()
    }
  },
  startListeningToStatus: function() {
    this.handleStatus()
    this.listenTo(
      this.model.get('query').get('result'),
      'sync request error',
      this.handleStatus
    )
  },
  handleStatus: function() {
    this.$el.toggleClass(
      'is-searching',
      this.model
        .get('query')
        .get('result')
        .isSearching()
    )
  },
  triggerRun: function(e) {
    this.model.get('query').startSearch()
    e.stopPropagation()
  },
  triggerStop: function(e) {
    this.model.get('query').cancelCurrentSearches()
    e.stopPropagation()
  },
  triggerDelete: function(e) {
    this.model.get('query').cancelCurrentSearches()
    this.model.collection.remove(this.model)
    e.stopPropagation()
  },
  triggerAdd(e) {
    lightboxInstance.model.updateTitle('Add List Items')
    lightboxInstance.model.open()
    lightboxInstance.showContent(
      new ListAddTabsView({
        extraHeaders: {
          'List-ID': this.model.attributes.id,
          'List-Type': this.model.get('list.icon'),
        },
        url: './internal/list/import',
        handleUploadSuccess: file => this.handleUploadSuccess(file),
        handleNewMetacard: id => this.handleNewMetacard(id),
        close: () => lightboxInstance.close(),
      })
    )
    e.stopPropagation()
  },
  handleNewMetacard(id) {
    if (id) {
      this.model.addBookmarks([id])
      this.model.get('query').startSearchIfOutdated()
    }
  },
  handleUploadSuccess(file) {
    var addedIds = file.xhr.getResponseHeader('Added-IDs')
    if (addedIds) {
      this.model.addBookmarks(addedIds.split(','))
      this.model.get('query').startSearchIfOutdated()
    }
  },
  serializeData: function() {
    return _merge(
      this.model.toJSON({
        additionalProperties: ['cid', 'color'],
      }),
      {
        icon: this.model.getIcon(),
      }
    )
  },
})
