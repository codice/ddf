/*global define, require */
/*jslint nomen:false, -W064 */

define(['config'], function () {
    require([
        'jquery',
        'backbone',
        'marionette',
        'application',
        'js/controllers/application.controller',
        'icanhaz',
        'js/HandlebarsHelpers'
    ], function ($, Backbone, Marionette, app, ApplicationController, ich) {
        'use strict';

        Marionette.Renderer.render = function (template, data) {
            if(!template){return '';}
            return ich[template](data);
        };

        // Set up the main regions that will be available at the Application level.
        app.App.addRegions({
            mainRegion : '#main',
            mapRegion : '#map',
            headerRegion : 'header',
            footerRegion : 'footer'
        });

        // Initialize the various services / controllers to be used by the application
        app.App.addInitializer(function() {
            app.Controllers.applicationController = new ApplicationController({app: app});
        });

        // Set up the frame of the application.
        app.App.addInitializer(function() {
            app.Controllers.applicationController.renderApplicationViews();
        });

        // Start up the Map view
        app.App.addInitializer(function() {
            app.showMapView();
        });

        // Once the application has been initialized (i.e. all initializers have completed), start up
        // Backbone.history.
        app.App.on('initialize:after', function() {
            Backbone.history.start();
        });
        
        app.App.addInitializer(function() {
            require(["js/Notification.module"]);
        });

        // Actually start up the application.
        app.App.start({});
    });
});