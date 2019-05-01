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
const _ = require('underscore')
const $ = require('jquery')
const CustomElements = require('../../js/CustomElements.js')
const queryItemView = require('./sort-item.view')
const template = require('../sort/sort.hbs')

module.exports = Marionette.CollectionView.extend({
  childView: queryItemView,
  tagName: CustomElements.register('sort-item-collection'),
  initialize: function(options) {
    if (this.collection.length === 0) {
      this.collection.add({
        attribute: 'title',
        direction: 'ascending',
      })
    }
  },
  childViewOptions: function(model, index) {
    return {
      collection: this.collection,
      childIndex: index,
      showBestTextOption: this.options.showBestTextOption,
    }
  },
})
