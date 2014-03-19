/* jshint unused: false */
/*global define*/
define(function(require) {

    var Application = require('js/application'),
        ServiceView = require('/configurations/view/Service.view.js');

    Application.App.module('Configurations', function(ServiceModule, App, Backbone, Marionette, $, _) {

            var servicePage = new ServiceView.ServicePage({model: Application.ServiceModel});

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