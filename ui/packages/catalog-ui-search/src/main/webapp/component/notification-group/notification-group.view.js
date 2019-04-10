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

var Marionette = require('marionette')
var CustomElements = require('../../js/CustomElements.js')
var template = require('./notification-group.hbs')
var NotificationListView = require('../notification-list/notification-list.view.js')
var userNotifications = require('../singletons/user-notifications.js')
var user = require('../singletons/user-instance.js')
var $ = require('jquery')
var Common = require('../../js/Common.js')

function isEmpty(filter) {
  return userNotifications.filter(filter).length === 0
}

function getNotifications(filter) {
  return userNotifications.filter(filter)
}

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('notification-group'),
  regions: {
    groupItems: '> .group-items',
  },
  events: {
    'click > .group-header .header-clear': 'handleClear',
    'click > .group-header .header-confirm': 'handleConfirm',
  },
  initialize: function() {
    this.handleEmpty()
    this.listenTo(userNotifications, 'add remove update', this.handleEmpty)
  },
  onBeforeShow: function() {
    this.groupItems.show(
      new NotificationListView({
        filter: this.options.filter,
      })
    )
  },
  handleEmpty: function() {
    var empty = isEmpty(this.options.filter)
    if (empty) {
      this.$el.css('height', this.$el.height())
    } else {
      this.$el.css('height', '')
    }
    Common.executeAfterRepaint(
      function() {
        this.$el.toggleClass('is-empty', empty)
      }.bind(this)
    )
  },
  handleClear: function(e) {
    this.$el.toggleClass('wait-for-confirmation', true)
    setTimeout(
      function() {
        this.listenForClick()
      }.bind(this),
      0
    )
  },
  listenForClick: function() {
    $(window).on(
      'click.notification-group',
      function(e) {
        this.$el.toggleClass('wait-for-confirmation', false)
        this.unlistenForClick()
      }.bind(this)
    )
  },
  unlistenForClick: function() {
    $(window).off('click.notification-group')
  },
  handleConfirm: function() {
    this.groupItems.currentView.children.forEach(function(childView) {
      childView.removeModel()
    })
  },
  serializeData: function() {
    return {
      date: this.options.date,
    }
  },
  onDestroy: function() {
    this.unlistenForClick()
  },
})
