/*global define, console, requirejs, require, window */
/*jslint nomen:false, -W064 */

(function () {
    'use strict';
    require(['config'], function () {
        require([
            'jquery',
            'backbone',
            'marionette',
            'js/application',
            'js/controllers/application.controller',
            'icanhaz'
        ], function ($, Backbone, Marionette, Application, ApplicationController) {
            var ddf = require('ddf'),
                app = ddf.app;

            Marionette.Renderer.render = function (template, data) {
                return ich[template](data);
            };

            // Set up the main regions that will be available at the Application level.
            app.addRegions({
                mainRegion : '#main',
                headerRegion : '#header',
                footerRegion : 'footer'
            });

            // Initialize the various services / controllers to be used by the application
            app.addInitializer(function() {
                app.controllers = {};
                app.controllers.applicationController = new ApplicationController({app: app});
            });

            // Set up the frame of the application.
            app.addInitializer(function() {
                app.controllers.applicationController.renderApplicationViews();

            });

            // Start up the main Application Router
            app.addInitializer(function() {
                app.router = new Application.Router();
            });

            // Once the application has been initialized (i.e. all initializers have completed), start up
            // Backbone.history.
            app.on('initialize:after', function() {
                Backbone.history.start();
                console.log('application running');
            });
            if(window){
                // make aviture object available on window.  Makes debugging in chrome console much easier
                window.ddf = ddf;
            }


            // Actually start up the application.
            app.start({});
        });
    });
}());