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
const userNotifications = require('../singletons/user-notifications.js')
const AlertItemView = require('../alert-item/alert-item.view.js')
const UploadItemView = require('../upload-batch-item/upload-batch-item.view.js')
const UploadItemModel = require('../../js/model/UploadBatch.js')
const OauthNotificationView = require('../oauth-item/oauth-item.view.js')
const OauthModel = require('../../js/model/Oauth.js')

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
  getChildView(model) {
    if (model.constructor === UploadItemModel) {
      return UploadItemView
    } else if (model.constructor === OauthModel) {
      return OauthNotificationView
    } else {
      return AlertItemView
    }
  },
  filter(model) {
    return matchesFilter(this.options.filter, model)
  },
})
