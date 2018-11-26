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
/*global require*/
var Marionette = require('marionette')
var CustomElements = require('../../js/CustomElements.js')
var userNotifications = require('../singletons/user-notifications.js')
var AlertItemView = require('../alert-item/alert-item.view.js')
var UploadItemView = require('../upload-batch-item/upload-batch-item.view.js')
var UploadItemModel = require('../../js/model/UploadBatch.js')

function matchesFilter(filter, model) {
  if (!filter) {
    return true
  } else {
    return true
  }
}

// polymorphic collection of notifications
module.exports = Marionette.CollectionView.extend({
  collection: userNotifications,
  className: 'is-list has-list-highlighting',
  tagName: CustomElements.register('notification-list'),
  getChildView: function(model) {
    if (model.constructor === UploadItemModel) {
      return UploadItemView
    } else {
      return AlertItemView
    }
  },
  filter: function(model) {
    return matchesFilter(this.options.filter, model)
  },
})
