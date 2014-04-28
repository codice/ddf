/* jshint unused: false */
/*global define*/
define(function(require) {

    var Application = require('js/application'),
        SourceView = require('/sources/js/view/Source.view.js'),
        poller = require('poller'),
        Source = require('/sources/js/model/Source.js');

    Application.App.module('Sources', function(SourceModule, App, Backbone, Marionette, $, _) {

        var Service = require('/sources/js/model/Service.js');

        var serviceModel = new Service.Response();
        serviceModel.fetch();
        var options = {
            delay: 30000
        };
        var servicePoller = poller.get(serviceModel, options);
        servicePoller.start();

        var sourceResponse = new Source.Response({model: serviceModel});

        var sourcePage = new SourceView.SourcePage({model: sourceResponse});

        // Define a controller to run this module
        // --------------------------------------

        var Controller = Marionette.Controller.extend({

            initialize: function(options){
                this.region = options.region;
            },

            show: function(){
                this.region.show(sourcePage);
            }

        });

        // Initialize this module when the app starts
        // ------------------------------------------

        SourceModule.addInitializer(function(){
            SourceModule.contentController = new Controller({
                region: App.sources
            });
            SourceModule.contentController.show();
        });


    });
});