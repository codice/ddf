/* global define */
define(function(require) {

    console.log("loading notification module");
    // Need to know where the StandardUiApp is
    var Application = require("application");
    var Cometd = require('cometdinit'); // I think this shouldn't be required in multiple places. It should be required once and given to the Application.

    // Create a new module in the StandardUiApp
    Application.App.module('NotificationModule', function(NotificationModule, StandardUiApp, Backbone, Marionette, $) {

        var NotificationView = require("js/view/Notification.view");
        var Notification = require("js/model/Notification");
        var NotificationCollection = require("js/model/Notifications");

        // Instantiate Notifications collection and pass that to the two views I will use.
        var notifications = new NotificationCollection();
        new NotificationView.NotificationListView({
            collection : notifications
        });

        NotificationModule.addInitializer(function() {

            this.subscription = Cometd.Comet.subscribe("/ddf/notification/**", function(resp) {
                // instead of doing a notifications.create(), make new Model and then add it to collection.
                // this is done to avoid calling model.save() which notifications.create() will do.
                var incomingNotification = new Notification($.parseJSON(resp.data));
                notifications.add(incomingNotification); // TODO:do I need to do any error checking if the JSON I get is malformed?
            });
        });
    });

});