/* global define */

define([ 'backbone', 'icanhaz', 'text!templates/notification/notification.message.handlebars', 'text!templates/notification/notification.title.handlebars', 'moment', 'underscore',
        'jquery', 'pnotify' ], function(Backbone, ich, messageTemplate, titleTemplate, moment, _, $) {

    // Create object to contain both the NotificationItemView and the NotificationListView in.
    // This is so we can return it below.
    var NotificationView = {};

    //var DATE_THRESHOLD = 2592000000; // thirty days ago

    ich.addTemplate('notificationTemplate', messageTemplate);
    ich.addTemplate('titleTemplate', titleTemplate);
    
    NotificationView.NotificationItemView = Backbone.Marionette.ItemView.extend({

        render : function() {

            var notification = $.pnotify({
                title : ich.titleTemplate(this.model.toJSON()), 
                text : this.constructNotificationText(),
                icon : "fa fa-download notification-title",
                hide : false,
                closer_hover : false,
                sticker: false, 
                history: false,
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
            return this;
        },

        constructNotificationText : function() {
            var formattedTime;

            // where should this happen?
            var modelTimestamp = this.model.get("timestamp");
            formattedTime = moment(modelTimestamp).format('MMMM Do YYYY, h:mm:ss a');
            this.model.set("formattedTime", formattedTime);

            var text = ich.notificationTemplate(this.model.toJSON()).html();
            return text;
        },

        onClose : function() {
            this.notification.pnotify_remove();
        }
    });

    // This loops through all models in provided collection. Renders each model.
    // Appends results of to collection view's el element. Which corresponds to the region it is placed in.
    NotificationView.NotificationListView = Backbone.Marionette.CollectionView.extend({
        itemView : NotificationView.NotificationItemView
    });

    return NotificationView;
});