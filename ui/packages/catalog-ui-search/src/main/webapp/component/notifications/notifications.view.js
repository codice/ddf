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
const template = require('./notification.hbs')
const CustomElements = require('../../js/CustomElements.js')
const NotificationEmpty = Marionette.ItemView.extend({
  className: 'notification-empty',
  template: 'No recent notifications.',
});

const NotificationItem = Marionette.ItemView.extend({
  template: template,
  className: 'notification',
  events: {
    'click .remove-notification': 'removeNotification',
  },
  initialize: function() {
    this.interval = setInterval(this.render.bind(this), 60000)
  },
  onDestroy: function() {
    clearInterval(this.interval)
  },
  modelEvents: {
    change: 'render',
  },
  removeNotification: function() {
    this.model.destroy()
  },
});

module.exports = Marionette.CollectionView.extend({
  tagName: CustomElements.register('notifications-list'),
  childView: NotificationItem,
  emptyView: NotificationEmpty,
  initialize: function() {
    this.collection = this.model
  },
})
