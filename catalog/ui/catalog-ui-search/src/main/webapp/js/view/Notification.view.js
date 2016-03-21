/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/* global define */
define([
    'backbone',
    'handlebars',
    'text!templates/notification/notification.message.handlebars',
    'text!templates/notification/notification.title.handlebars',
    'underscore',
    'jquery',
    'wreqr',
    'moment',
    'pnotify'
], function (Backbone, hbs, messageTemplate, titleTemplate, _, $, wreqr, moment) {
    // Create object to contain both the NotificationItemView and the NotificationListView in.
    // This is so we can return it below.
    var NotificationView = {};
    var currentTime = moment();

    var message = hbs.compile(messageTemplate);
    var title = hbs.compile(titleTemplate);

    //notificationStack and isNotificationOpen used to display only one popup notification at a time
    var notificationStack = [];
    var isNotificationOpen = false;
    NotificationView.NotificationItemView = Backbone.Marionette.ItemView.extend({
        initialize: function () {
            this.listenTo(wreqr.vent, 'notification:open', this.openNotification);
        },
        onRender: function () {
            var notificationTime = moment(this.model.get('timestamp'));
            if (notificationTime.diff(currentTime) > 0) {
                this.createNotification();
            }
            return this;
        },
        createNotification: function () {
            var notification = $.pnotify({
                title: title(this.model.toJSON()),
                text: this.constructNotificationText(),
                icon: 'fa fa-exclamation-circle notification-title',
                nonblock: true,
                auto_display: false,
                hide: true,
                delay: 3000,
                history: false,
                after_close: function () {
                    if (notificationStack.length) {
                        notificationStack.shift().pnotify_display();
                    } else {
                        isNotificationOpen = false;
                    }
                },
                stack: {
                    'dir1': 'up',
                    'dir2': 'left',
                    'spacing1': 0,
                    'spacing2': 0,
                    'firstpos1': 25,
                    'firstpos2': 25
                },
                //need to define custom styling since the default pnotify fontawesome styling makes every notice a warning.
                styling: {
                    container: 'alert',
                    notice: '',
                    notice_icon: 'fa fa-exclamation-circle',
                    info: 'alert-info',
                    info_icon: 'fa fa-info',
                    success: 'alert-success',
                    success_icon: 'fa fa-check',
                    error: 'alert-danger',
                    error_icon: 'fa fa-warning',
                    pin_up: 'fa fa-pause',
                    pin_down: 'fa fa-play',
                    hi_menu: 'well',
                    hi_btn: 'btn btn-default',
                    hi_btnhov: '',
                    hi_hnd: 'fa fa-chevron-down'
                }
            });
            if (isNotificationOpen === false) {
                this.notification = notification;
                this.notification.pnotify_display();
                isNotificationOpen = true;
            } else {
                notificationStack.push(notification);
            }
        },
        openNotification: function (noti) {
            if (noti && noti.cid === this.model.cid) {
                if (!this.notification) {
                    this.createNotification();
                } else {
                    this.notification.pnotify_display();
                }
            }
        },
        constructNotificationText: function () {
            return message(this.model.toJSON());
        },
        onDestroy: function () {
            if (this.notification) {
                this.notification.pnotify_remove();
            }
        }
    });
    // This loops through all models in provided collection. Renders each model.
    // Appends results of to collection view's el element. Which corresponds to the region it is placed in.
    NotificationView.NotificationListView = Backbone.Marionette.CollectionView.extend({ childView: NotificationView.NotificationItemView });
    return NotificationView;
});
