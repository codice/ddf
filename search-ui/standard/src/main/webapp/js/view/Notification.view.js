/* global define */

define([ 'backbone', 
         'icanhaz', 
         'text!templates/notification/notification.message.handlebars', 
         'text!templates/notification/notification.title.handlebars', 
         'underscore',
         'jquery', 
         'pnotify' 
       ], 
         function(Backbone, ich, messageTemplate, titleTemplate, _, $) {

    // Create object to contain both the NotificationItemView and the NotificationListView in.
    // This is so we can return it below.
    var NotificationView = {};

    ich.addTemplate('notificationTemplate', messageTemplate);
    ich.addTemplate('titleTemplate', titleTemplate);
    
    var stack_context;
    
    NotificationView.NotificationItemView = Backbone.Marionette.ItemView.extend({

        onRender : function() {
            if (typeof stack_context === "undefined") stack_context = {
                    "dir1": "down",
                    "dir2": "left",
                    "context": $("#notifications")
            };
            
            var notification = $.pnotify({
                title : ich.titleTemplate(this.model.toJSON()), 
                text : this.constructNotificationText(),
                icon : "fa fa-exclamation-circle notification-title",  
                hide : false,
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
            return this;
        },

        constructNotificationText : function() {
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