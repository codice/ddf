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
/*jslint nomen:false, -W064, browser:true */

(function () {
    'use strict';

    require.config({

        paths: {

            bootstrap: '/webapp/libs/bootstrap/3.2.0/dist/js/bootstrap.min',
            spin: '/webapp/libs/spin.js/1.3.3/spin',
            q: '/webapp/libs/q/1.0.1/q',

            // backbone
            backbone: '/webapp/libs/backbone/1.1.2/backbone',
            backbonerelational: '/webapp/libs/backbone-relational/0.8.8/backbone-relational',
            underscore: '/webapp/libs/lodash/2.4.1/dist/lodash.underscore.min',
            marionette: '/webapp/libs/marionette/1.8.8/lib/backbone.marionette.min',
            // TODO test combining
            modelbinder: '/webapp/libs/backbone.modelbinder/1.1.0/Backbone.ModelBinder.min',
            collectionbinder: '/webapp/libs/backbone.modelbinder/1.1.0/Backbone.CollectionBinder.min',
            poller: '/webapp/libs/backbone-poller/1.1.3/backbone.poller',

            // ddf
            spinnerConfig: 'js/spinnerConfig',

            // jquery
            jquery: '/webapp/libs/jquery/1.12.4/dist/jquery.min',
            jsCookie: '/webapp/libs/js-cookie/2.1.1/src/js.cookie',
            jqueryui: '/webapp/libs/jquery-ui/1.10.4/ui/minified/jquery-ui.min',
            'jquery.ui.widget': '/webapp/libs/jquery-ui/1.10.4/ui/minified/jquery.ui.widget.min',
            multiselect: '/webapp/libs/bootstrap-multiselect/0.9.3/js/bootstrap-multiselect',
            perfectscrollbar: '/webapp/libs/perfect-scrollbar/0.4.8/min/perfect-scrollbar-0.4.8.with-mousewheel.min',
            fileupload: '/webapp/libs/jquery-file-upload/9.5.7/js/jquery.fileupload',
            fileuploadiframe: '/webapp/libs/jquery-file-upload/9.5.7/js/jquery.iframe-transport',

            // handlebars
            handlebars: '/webapp/libs/handlebars/2.0.0/handlebars.min',
            icanhaz: 'js/ich',

            // require plugins
            text: '/webapp/libs/requirejs-plugins/1.0.2/lib/text',
            css: '/webapp/libs/require-css/0.1.5/css',

            // default admin ui
            app: 'js/application',

            //moment
            moment: '/webapp/libs/moment/2.5.1/moment',


            //iframe-resizer
            iframeresizer: '/webapp/libs/iframe-resizer/2.6.2/js/iframeResizer.min',

            //backbone assocations
            backboneassociations: '/webapp/libs/backbone-associations/0.6.2/backbone-associations-min'
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
        'js/util/SessionRefresherUtil',
        'icanhaz',
        'js/HandlebarsHelpers',
        'modelbinder',
        'bootstrap',
        'templateConfig'
    ], function ($, Backbone, Marionette, Application, ModuleView, AlertsModel, AlertsView, Properties, SessionRefresherUtil) {

        var app = Application.App;

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

        //refresh the session if keypress  or mouse click events occur
        SessionRefresherUtil(60000);

        //redirect upon session timeout
        $(document).ajaxError(function (_, jqxhr) {
                if (jqxhr.status === 401 || jqxhr.status === 403) {
                    app.addInitializer(function () {
                        Application.App.alertsRegion.show(new AlertsView.View({'model': AlertsModel.Jolokia({'stacktrace': 'Forbidden'})}));
                    });
                }

            }
        );

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