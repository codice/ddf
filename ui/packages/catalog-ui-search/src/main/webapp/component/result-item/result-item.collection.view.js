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
/*global define, alert*/
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const CustomElements = require('../../js/CustomElements.js')
const childView = require('./result-item.view')
const groupView = require('../result-group/result-group.view.js')
const store = require('../../js/store.js')
const _debounce = require('lodash/debounce')

module.exports = Marionette.CollectionView.extend({
  emptyView: Marionette.ItemView.extend({
    className: 'result-item-collection-empty',
    template: 'No results found',
  }),
  tagName: CustomElements.register('result-item-collection'),
  getChildView: function(childModel) {
    if (childModel.duplicates && !this.options.group) {
      return groupView
    } else {
      return childView
    }
  },
  childViewOptions: function() {
    return {
      selectionInterface: this.options.selectionInterface,
    }
  },
  className: 'is-list has-list-highlighting',
  selectionInterface: store,
  initialize: function(options) {
    this.selectionInterface = options.selectionInterface || store
    this.render = _debounce(this.render, 200)
  },
  render: function() {
    if (this.isDestroyed) {
      return // not likely, but possible with the debounce that the view could get destroyed
    }
    Marionette.CollectionView.prototype.render.call(this)
  },
  onAddChild: function(childView) {
    childView.$el.attr('data-index', this.children.length - 1)
  },
})
