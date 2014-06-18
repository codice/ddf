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

define([ 'backbone', 
         'icanhaz', 
         'text!templates/notification/notification.message.handlebars', 
         'text!templates/notification/notification.title.handlebars', 
         'underscore',
         'jquery',
         'wreqr',
         'moment',
         'pnotify'
       ], 
         function(Backbone, ich, messageTemplate, titleTemplate, _, $, wreqr, moment) {

    // Create object to contain both the NotificationItemView and the NotificationListView in.
    // This is so we can return it below.
    var NotificationView = {};

    ich.addTemplate('notificationTemplate', messageTemplate);
    ich.addTemplate('titleTemplate', titleTemplate);
    
    var stack_context;

    var currentTime = moment();
    
    NotificationView.NotificationItemView = Backbone.Marionette.ItemView.extend({

        initialize: function() {
            wreqr.vent.on('notification:open', _.bind(this.openNotification, this));
        },

        onRender : function() {
            var notificationTime = moment(this.model.get("timestamp"));
            if(notificationTime.diff(currentTime) > 0) {
                this.createNotification();
            }

            return this;
        },

        createNotification: function() {
            if (typeof stack_context === "undefined") stack_context = {
                "dir1": "down",
                "dir2": "left",
                "context": $("#notifications")
            };

            var notification = $.pnotify({
                title : ich.titleTemplate(this.model.toJSON()),
                text : this.constructNotificationText(),
                icon : "fa fa-exclamation-circle notification-title",
                hide : true,
                closer_hover : false,
                sticker: false,
                history: false,
                stack: stack_context,
                //need to define custom styling since the default pnotify fontawesome styling makes every notice a warning.
                styling: {
                    container: "alert",
                    notice: "",
                    notice_icon: "fa fa-exclamation-circle",
                    info: "alert-info",
                    info_icon: "fa fa-info",
                    success: "alert-success",
                    success_icon: "fa fa-check",
                    error: "alert-danger",
                    error_icon: "fa fa-warning",
                    closer: "fa fa-times",
                    pin_up: "fa fa-pause",
                    pin_down: "fa fa-play",
                    hi_menu: "well",
                    hi_btn: "btn btn-default",
                    hi_btnhov: "",
                    hi_hnd: "fa fa-chevron-down"
                }
            });
            this.notification = notification;
        },

        openNotification: function(noti) {
            if(noti && noti.cid === this.model.cid) {
                if(!this.notification) {
                    this.createNotification();
                } else {
                    this.notification.pnotify_display();
                }
            }
        },

        constructNotificationText : function() {
            var text = ich.notificationTemplate(this.model.toJSON()).html();
            return text;
        },

        onClose : function() {
            if(this.notification) {
                this.notification.pnotify_remove();
            }
        }
    });

    // This loops through all models in provided collection. Renders each model.
    // Appends results of to collection view's el element. Which corresponds to the region it is placed in.
    NotificationView.NotificationListView = Backbone.Marionette.CollectionView.extend({
        itemView : NotificationView.NotificationItemView
    });

    return NotificationView;
});
