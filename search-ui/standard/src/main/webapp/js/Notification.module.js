/* global define */
define(["application", 
        "cometdinit", 
        "js/model/Notification", 
        "js/view/Notification.view",
        'wreqr'],
        function(Application, Cometd, Notification, NotificationView, wreqr) {

    // Create a new module in the StandardUiApp
    Application.App.module('NotificationModule', function(NotificationModule) {

        // Instantiate Notifications collection and pass that to the two views I will use.
        var notifications = new Notification.List();
        new NotificationView.NotificationListView({
            collection : notifications
        });

        wreqr.reqres.setHandler('notifications', function () {
            return notifications;
        });

        NotificationModule.addInitializer(function() {

            this.subscription = Cometd.Comet.subscribe("/ddf/notifications/**", function(resp) {
                // instead of doing a notifications.create(), make new Model and then add it to collection.
                // this is done to avoid calling model.save() which notifications.create() will do.
                var incomingNotification = new Notification.Notification(resp, {validate: true, parse: true});
                
                //only add the Notification if it is properly formatted and contains application, message, and title
                if(!incomingNotification.validationError){
                    notifications.add(incomingNotification);
                    wreqr.vent.trigger('notification:new', incomingNotification);
                }
            });
        });
    });

});