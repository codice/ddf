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
/*global require, window */
/*jslint nomen:false, -W064 */

(function () {
    'use strict';

    require.config({

        paths: {

            bootstrap: 'lib/bootstrap/dist/js/bootstrap.min',
            spin: 'lib/spin.js/spin',
            q: 'lib/q/q',

            // backbone
            backbone: 'lib/backbone/backbone',
            backbonerelational: 'lib/backbone-relational/backbone-relational',
            underscore: 'lib/lodash/dist/lodash.underscore.min',
            marionette: 'lib/marionette/lib/backbone.marionette.min',
            // TODO test combining
            modelbinder: 'lib/backbone.modelbinder/Backbone.ModelBinder.min',
            collectionbinder: 'lib/backbone.modelbinder/Backbone.CollectionBinder.min',
            poller: 'lib/backbone-poller/backbone.poller',

            // ddf
            spinnerConfig: 'js/spinnerConfig',

            // jquery
            jquery: 'lib/jquery/dist/jquery.min',
            jsCookie: 'lib/js-cookie/src/js.cookie',
            jqueryui: 'lib/jquery-ui/ui/minified/jquery-ui.min',
            'jquery.ui.widget': 'lib/jquery-ui/ui/minified/jquery.ui.widget.min',
            multiselect: 'lib/bootstrap-multiselect/js/bootstrap-multiselect',
            perfectscrollbar: 'lib/perfect-scrollbar/min/perfect-scrollbar-0.4.8.with-mousewheel.min',
            fileupload: 'lib/jquery-file-upload/js/jquery.fileupload',
            fileuploadiframe: 'lib/jquery-file-upload/js/jquery.iframe-transport',

            // handlebars
            handlebars: 'lib/handlebars/handlebars.min',
            icanhaz: 'js/ich',

            // require plugins
            text: 'lib/requirejs-plugins/lib/text',
            css: 'lib/require-css/css',

            // default admin ui
            app: 'js/application',

            //moment
            moment: 'lib/moment/moment',


            //iframe-resizer
            iframeresizer: 'lib/iframe-resizer/js/iframeResizer.min',

            //backbone assocations
            backboneassociations: 'lib/backbone-associations/backbone-associations-min'
        },


        shim: {

            backbone: {
                deps: ['underscore', 'jquery'],
                exports: 'Backbone'
            },
            modelbinder: {
                deps: ['underscore', 'jquery', 'backbone']
            },
            collectionbinder: {
                deps: ['modelbinder']
            },
            poller: {
                deps: ['underscore', 'backbone']
            },
            backbonerelational: ['backbone'],

            marionette: {
                deps: ['jquery', 'underscore', 'backbone'],
                exports: 'Marionette'
            },
            underscore: {
                exports: '_'
            },
            handlebars: {
                exports: 'Handlebars'
            },
            icanhaz: {
                deps: ['handlebars', 'jquery'],
                exports: 'ich'
            },

            perfectscrollbar: ['jquery'],

            multiselect: ['jquery', 'jquery.ui.widget'],
            fileupload: ['jquery', 'jquery.ui.widget'],

            jqueryui: ['jquery'],
            jsCookie: ['jquery'],
            bootstrap: ['jqueryui'],
            moment: {exports: 'moment'},

            backboneassociations: ['backbone']

        },

        waitSeconds: 200
    });


    require([
        'jquery',
        'backbone',
        'marionette',
        'js/application',
        'js/views/Module.view',
        'js/models/Alerts.js',
        'js/views/Alerts.view',
        'properties',
        'icanhaz',
        'js/HandlebarsHelpers',
        'modelbinder',
        'bootstrap',
        'templateConfig'
    ], function ($, Backbone, Marionette, Application, ModuleView, AlertsModel, AlertsView, Properties) {

        var app = Application.App;

        Application.AppModel = new Backbone.Model(Properties);

        //setup the area that the modules will load into and asynchronously require in each module
        //so that it can render itself into the area that was just constructed for it
        app.addInitializer(function () {
            Application.App.mainRegion.show(new ModuleView({model: Application.ModuleModel}));
        });

        //setup the header
        app.addInitializer(function () {
            if (Properties.ui.header && Properties.ui.header !== '') {
                $('html').addClass('has-header');
            }
            Application.App.headerRegion.show(new Marionette.ItemView({
                template: 'headerLayout',
                className: 'header-layout',
                model: Application.AppModel
            }));
        });

        var alerts = new AlertsModel.InsecureAlerts();

        alerts.fetch({
            success: function () {
                app.addInitializer(function () {
                    Application.App.alertsRegion.show(new AlertsView.View({
                        model: alerts
                    }));
                });
            }
        });

        //setup the footer
        app.addInitializer(function () {
            if (Properties.ui.footer && Properties.ui.footer !== '') {
                $('html').addClass('has-footer');
            }
            Application.App.footerRegion.show(new Marionette.ItemView({
                template: 'footerLayout',
                className: 'footer-layout',
                model: Application.AppModel
            }));
        });

        // Start up the main Application Router
        app.addInitializer(function () {
            app.router = new Application.Router();
        });

        // Once the application has been initialized (i.e. all initializers have completed), start up
        // Backbone.history.
        app.on('initialize:after', function () {
            Backbone.history.start();
            //bootstrap call for tabs
            $('tabs').tab();
        });

        if (window) {
            // make ddf object available on window.  Makes debugging in chrome console much easier
            window.app = app;
            if (!window.console) {
                window.console = {
                    log: function () {
                        //no op
                    }
                };
            }
        }

        // Actually start up the application.
        app.start();
    });
}());