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
const childView = require('./upload-batch-item.view')
const CustomElements = require('../../js/CustomElements.js')
const user = require('../singletons/user-instance.js')

module.exports = Marionette.CollectionView.extend({
  emptyView: Marionette.ItemView.extend({
    className: 'alert-empty',
    template: 'No Recent Uploads',
  }),
  className: 'is-list has-list-highlighting',
  setDefaultCollection() {
    this.collection = user
      .get('user')
      .get('preferences')
      .get('uploads')
  },
  childView,
  tagName: CustomElements.register('upload-batch-item-collection'),
  initialize(options) {
    if (!options.collection) {
      this.setDefaultCollection()
    }
  },
})
