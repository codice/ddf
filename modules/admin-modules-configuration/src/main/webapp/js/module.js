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
/* jshint unused: false */
/*global define*/
define(['js/application','poller','/configurations/js/view/Service.view.js','/configurations/js/model/Service.js'],function(Application,poller,ServiceView,Service) {

    Application.App.module('Configurations', function(ServiceModule, App, Backbone, Marionette, $, _) {

            var serviceModel = new Service.Response();
            serviceModel.fetch();
            var options = {
                delay: 10000
            };
            var servicePoller = poller.get(serviceModel, options);
            servicePoller.start();

            var servicePage = new ServiceView.ServicePage({model: serviceModel, poller: servicePoller});

            // Define a controller to run this module
            // --------------------------------------

            var Controller = Marionette.Controller.extend({

                initialize: function(options){
                    this.region = options.region;
                },

                show: function(){
                    this.region.show(servicePage);
                }

            });

            // Initialize this module when the app starts
            // ------------------------------------------

            ServiceModule.addInitializer(function(){
                ServiceModule.contentController = new Controller({
                    region: App.configurations
                });
                ServiceModule.contentController.show();
            });


    });
});