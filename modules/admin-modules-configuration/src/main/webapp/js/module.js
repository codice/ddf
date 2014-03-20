/* jshint unused: false */
/*global define*/
define(function(require) {

    var Application = require('js/application'),
        poller = require('poller'),
        ServiceView = require('/configurations/js/view/Service.view.js');

    Application.App.module('Configurations', function(ServiceModule, App, Backbone, Marionette, $, _) {

            var Service = require('/configurations/js/model/Service.js');

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