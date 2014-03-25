/*global require, window */
/*jslint nomen:false, -W064 */

(function () {
    'use strict';

    require(['config'], function () {
        require([
            'jquery',
            'backbone',
            'marionette',
            'icanhaz',
            'js/application',
            'js/views/Module.view',
            'js/HandlebarsHelpers',
            'modelbinder',
            'bootstrap'

        ], function ($, Backbone, Marionette, ich, Application, ModuleView) {

            var app = Application.App;

            Marionette.Renderer.render = function (template, data) {
                if(!template){return '';}
                return ich[template](data);
            };

            //setup the area that the modules will load into and asynchronously require in each module
            //so that it can render itself into the area that was just constructed for it
            app.addInitializer(function() {
                Application.App.mainRegion.show(new ModuleView({model: Application.ModuleModel}));
            });

            //setup the header
            app.addInitializer(function() {
                Application.App.headerRegion.show(new Marionette.ItemView({
                    template: 'headerLayout',
                    className: 'header-layout',
                    model: Application.AppModel
                }));
            });

            //setup the footer
            app.addInitializer(function() {
                Application.App.footerRegion.show(new Marionette.ItemView({
                    template: 'footerLayout',
                    className: 'footer-layout',
                    model: Application.AppModel
                }));
            });

            // Start up the main Application Router
            app.addInitializer(function() {
                app.router = new Application.Router();
            });

            // Once the application has been initialized (i.e. all initializers have completed), start up
            // Backbone.history.
            app.on('initialize:after', function() {
                Backbone.history.start();
                //bootstrap call for tabs
                $('tabs').tab();
            });

            if(window){
                // make ddf object available on window.  Makes debugging in chrome console much easier
                window.app = app;
            }

            // Actually start up the application.
            app.start();
        });
    });
}());