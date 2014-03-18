/* jshint unused: false */
/*global define*/
define(function(require) {

    var Application = require('js/application'),
        AppView = require('js/views/App.view'),
        AppModel = require('js/models/App');

    Application.App.module('AppModule', function(AppModule, App, Backbone, Marionette, $, _) {

        var appModel = new AppModel.Model();

        // Define a view to show
        // ---------------------

        var appPage = new AppView({model: appModel});

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

        //            contentRegion: '#content-region'
        // Initialize this module when the app starts
        // ------------------------------------------

        AppModule.addInitializer(function(){
            AppModule.contentController = new Controller({
                region: App.mainRegion
            });
            AppModule.contentController.show();
        });


    });
});