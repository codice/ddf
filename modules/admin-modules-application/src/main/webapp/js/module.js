/* jshint unused: false */
/*global define*/
define([
    'js/application',
    'require',
    '/applications/templateConfig.js'
    ],function(Application, require) {


    Application.App.module('Applications', function(ApplicationModule, App, Backbone, Marionette, $, _) {

        require([
                '/applications/js/view/ApplicationWrapper.view.js',
                '/applications/js/model/Applications.js'
            ], function(ApplicationView, ApplicationModel) {
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
});