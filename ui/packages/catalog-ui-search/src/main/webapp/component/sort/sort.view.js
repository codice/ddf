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
const SortItemCollectionView = require('../sort-item/sort-item.collection.view.js')
const template = require('./sort.hbs')

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('sort'),
  regions: {
    sorts: '.sorts',
  },
  events: {
    'click .sort-add': 'handleAdd',
  },
  handleAdd: function() {
    this.childView.collection.add({
      attribute: this.getNextAttribute(),
      direction: 'descending',
    })
  },
  getNextAttribute: function() {
    let filtered = this.childView.children
      .findByModel(this.collection.models[0])
      .sortAttributes.filter(type => {
        let sorts = this.childView.collection.filter(sort => {
          return sort.get('attribute') === type.value
        })
        return sorts.length === 0
      })
    return filtered[0].value
  },
  initialize: function() {
    this.childView = new SortItemCollectionView({
      collection: this.collection,
      showBestTextOption: this.options.showBestTextOption,
    })
  },
  onBeforeShow: function() {
    this.showChildView('sorts', this.childView)
  },
})
