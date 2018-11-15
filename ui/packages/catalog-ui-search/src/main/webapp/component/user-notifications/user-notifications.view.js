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
var Backbone = require('backbone')
var template = require('./user-notifications.hbs')
var CustomElements = require('../../js/CustomElements.js')
var NotificationGroupView = require('../notification-group/notification-group.view.js')
var user = require('../singletons/user-instance.js')
var moment = require('moment')
var userNotifications = require('../singletons/user-notifications.js')

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('user-notifications'),
  regions: {
    listToday: '> .notifications-list > .list-today',
    listPreviousOne: '> .notifications-list > .list-previous-one',
    listPreviousTwo: '> .notifications-list > .list-previous-two',
    listPreviousThree: '> .notifications-list > .list-previous-three',
    listPreviousFour: '> .notifications-list > .list-previous-four',
    listPreviousFive: '> .notifications-list > .list-previous-five',
    listPreviousSix: '> .notifications-list > .list-previous-six',
    listOlder: '> .notifications-list > .list-older',
  },
  initialize: function() {
    this.listenTo(userNotifications, 'add remove update', this.handleEmpty)
    this.handleEmpty()
  },
  handleEmpty: function() {
    this.$el.toggleClass('is-empty', userNotifications.length === 0)
  },
  onBeforeShow: function() {
    this.listToday.show(
      new NotificationGroupView({
        filter: function(model) {
          return moment().diff(model.get('sentAt'), 'days') === 0
        },
        date: 'Today',
      }),
      {
        replaceElement: true,
      }
    )
    this.listPreviousOne.show(
      new NotificationGroupView({
        filter: function(model) {
          return moment().diff(model.get('sentAt'), 'days') === 1
        },
        date: 'Yesterday',
      }),
      {
        replaceElement: true,
      }
    )
    this.listPreviousTwo.show(
      new NotificationGroupView({
        filter: function(model) {
          return moment().diff(model.get('sentAt'), 'days') === 2
        },
        date: moment()
          .subtract(2, 'days')
          .format('dddd'),
      }),
      {
        replaceElement: true,
      }
    )
    this.listPreviousThree.show(
      new NotificationGroupView({
        filter: function(model) {
          return moment().diff(model.get('sentAt'), 'days') === 3
        },
        date: moment()
          .subtract(3, 'days')
          .format('dddd'),
      }),
      {
        replaceElement: true,
      }
    )
    this.listPreviousFour.show(
      new NotificationGroupView({
        filter: function(model) {
          return moment().diff(model.get('sentAt'), 'days') === 4
        },
        date: moment()
          .subtract(4, 'days')
          .format('dddd'),
      }),
      {
        replaceElement: true,
      }
    )
    this.listPreviousFive.show(
      new NotificationGroupView({
        filter: function(model) {
          return moment().diff(model.get('sentAt'), 'days') === 5
        },
        date: moment()
          .subtract(5, 'days')
          .format('dddd'),
      }),
      {
        replaceElement: true,
      }
    )
    this.listPreviousSix.show(
      new NotificationGroupView({
        filter: function(model) {
          return moment().diff(model.get('sentAt'), 'days') === 6
        },
        date: moment()
          .subtract(6, 'days')
          .format('dddd'),
      }),
      {
        replaceElement: true,
      }
    )
    this.listOlder.show(
      new NotificationGroupView({
        filter: function(model) {
          return moment().diff(model.get('sentAt'), 'days') >= 7
        },
        date: 'Older',
      }),
      {
        replaceElement: true,
      }
    )
  },
  onDestroy: function() {
    userNotifications.setSeen()
    user
      .get('user')
      .get('preferences')
      .savePreferences()
  },
})
