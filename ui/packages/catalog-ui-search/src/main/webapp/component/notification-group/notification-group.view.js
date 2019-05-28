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
const template = require('./notification-group.hbs')
const NotificationListView = require('../notification-list/notification-list.view.js')
const userNotifications = require('../singletons/user-notifications.js')
const $ = require('jquery')
const Common = require('../../js/Common.js')

function isEmpty(filter) {
  return userNotifications.filter(filter).length === 0
}

function getNotifications(filter) {
  return userNotifications.filter(filter)
}

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('notification-group'),
  regions: {
    groupItems: '> .group-items',
  },
  events: {
    'click > .group-header .header-clear': 'handleClear',
    'click > .group-header .header-confirm': 'handleConfirm',
  },
  initialize() {
    this.handleEmpty()
    this.listenTo(userNotifications, 'add remove update', this.handleEmpty)
  },
  onBeforeShow() {
    this.groupItems.show(
      new NotificationListView({
        filter: this.options.filter,
      })
    )
  },
  handleEmpty() {
    const empty = isEmpty(this.options.filter)
    if (empty) {
      this.$el.css('height', this.$el.height())
    } else {
      this.$el.css('height', '')
    }
    Common.executeAfterRepaint(() => {
      this.$el.toggleClass('is-empty', empty)
    })
  },
  handleClear(e) {
    this.$el.toggleClass('wait-for-confirmation', true)
    setTimeout(() => {
      this.listenForClick()
    }, 0)
  },
  listenForClick() {
    $(window).on('click.notification-group', e => {
      this.$el.toggleClass('wait-for-confirmation', false)
      this.unlistenForClick()
    })
  },
  unlistenForClick() {
    $(window).off('click.notification-group')
  },
  handleConfirm() {
    this.groupItems.currentView.children.forEach(childView => {
      childView.removeModel()
    })
  },
  serializeData() {
    return {
      date: this.options.date,
    }
  },
  onDestroy() {
    this.unlistenForClick()
  },
})
