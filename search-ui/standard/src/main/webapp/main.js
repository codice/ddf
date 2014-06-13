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
            footerRegion : 'footer',
            menuRegion: '#menu'
        });

        // Initialize the various services / controllers to be used by the application
        app.App.addInitializer(function() {
            app.Controllers.applicationController = new ApplicationController({app: app});
        });

        // Set up the frame of the application.
        app.App.addInitializer(function() {
            app.Controllers.applicationController.renderApplicationViews();
        });

        //setup the header
        app.App.addInitializer(function() {
            app.App.headerRegion.show(new Marionette.ItemView({
                template: 'headerLayout',
                className: 'header-layout',
                model: app.AppModel
            }));
        });

        //setup the footer
        app.App.addInitializer(function() {
            app.App.footerRegion.show(new Marionette.ItemView({
                template: 'footerLayout',
                className: 'footer-layout',
                model: app.AppModel
            }));
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

        app.App.addInitializer(function() {
            require(["js/Tasks.module"]);
        });

        // Actually start up the application.
        app.App.start({});
    });
});