/* jshint unused: false */
/*global define*/
define(function(require) {

    var Application = require('js/application'),
        poller = require('poller'),
        ApplicationView = require('/applications/js/view/Application.view.js');

    Application.App.module('Applications', function(ApplicationModule, App, Backbone, Marionette, $, _) {

            var ApplicationModel = require('/applications/js/model/Applications.js');

//            var appModel = new ApplicationModel.Response();
//            appModel.fetch();

            var appPage = new ApplicationView({modelClass: ApplicationModel});

            // Define a controller to run this module
            // --------------------------------------

            var Controller = Marionette.Controller.extend({

                initialize: function(options){
                    this.region = options.region;
                },

                show: function(){
                    this.region.show(appPage);
                }

            });

            // Initialize this module when the app starts
            // ------------------------------------------

            ApplicationModule.addInitializer(function(){
                ApplicationModule.contentController = new Controller({
                    region: App.applications
                });
                ApplicationModule.contentController.show();
            });


    });
});