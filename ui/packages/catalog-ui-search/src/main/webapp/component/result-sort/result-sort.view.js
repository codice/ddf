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
/*global define*/
const Marionette = require('marionette')
const Backbone = require('backbone')
const _ = require('underscore')
const $ = require('jquery')
const template = require('./result-sort.hbs')
const CustomElements = require('../../js/CustomElements.js')
const user = require('../singletons/user-instance.js')
const SortItemCollectionView = require('../sort/sort.view.js')

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('result-sort'),
  modelEvents: {
    change: 'render',
  },
  events: {
    'click > .editor-footer .footer-remove': 'removeSort',
    'click > .editor-footer .footer-save': 'saveSort',
  },
  ui: {},
  regions: {
    editorProperties: '.editor-properties',
  },
  initialize: function() {},
  onRender: function() {
    var resultSort = user
      .get('user')
      .get('preferences')
      .get('resultSort')
    this.editorProperties.show(
      new SortItemCollectionView({
        collection: new Backbone.Collection(resultSort),
      })
    )
    this.handleSort()
  },
  removeSort: function() {
    user
      .get('user')
      .get('preferences')
      .set('resultSort', undefined)
    user
      .get('user')
      .get('preferences')
      .savePreferences()
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
  saveSort: function() {
    var sorting = this.editorProperties.currentView.collection.toJSON()
    user
      .get('user')
      .get('preferences')
      .set('resultSort', sorting.length === 0 ? undefined : sorting)
    user
      .get('user')
      .get('preferences')
      .savePreferences()
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
  handleSort: function() {
    var resultSort = user
      .get('user')
      .get('preferences')
      .get('resultSort')
    this.$el.toggleClass('has-sort', Boolean(resultSort))
  },
})
