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

            bootstrap: 'bootstrap/3.3.7/dist/js/bootstrap.min',
            spin: 'spin.js/1.3.3/spin',
            q: 'q/1.4.1/q',

            // backbone
            backbone: 'backbone/1.1.2/backbone',
            backbonerelational: 'backbone-relational/0.8.8/backbone-relational',
            underscore: 'underscore/1.8.3/underscore-min',
            marionette: 'marionette/1.8.8/lib/backbone.marionette.min',
            // TODO test combining
            modelbinder: 'backbone.modelbinder/1.1.0/Backbone.ModelBinder.min',
            collectionbinder: 'backbone.modelbinder/1.1.0/Backbone.CollectionBinder.min',
            poller: 'backbone-poller/1.1.3/backbone.poller',

            // ddf
            spinnerConfig: 'js/spinnerConfig',

            // jquery
            jquery: 'jquery/3.2.1/dist/jquery.min',
            jsCookie: 'js-cookie/2.1.4/src/js.cookie',
            jqueryui: 'jquery-ui/1.12.1/jquery-ui.min',
            multiselect: 'bootstrap-multiselect/0.9.3/js/bootstrap-multiselect',
            perfectscrollbar: 'perfect-scrollbar/0.7.0/js/perfect-scrollbar.jquery.min',
            fileupload: 'jquery-file-upload/9.5.7/js/jquery.fileupload',
            fileuploadiframe: 'jquery-file-upload/9.5.7/js/jquery.iframe-transport',

            // handlebars
            handlebars: 'handlebars/2.0.0/handlebars.min',
            icanhaz: 'js/ich',

            // require plugins
            text: 'requirejs-plugins/1.0.3/lib/text',
            css: 'require-css/0.1.10/css',

            // default admin ui
            app: 'js/application',

            //moment
            moment: 'moment/2.20.1/moment',


            //iframe-resizer
            iframeresizer: 'iframe-resizer/2.6.2/js/iframeResizer.min',

            //backbone assocations
            backboneassociations: 'backbone-associations/0.6.2/backbone-associations-min'
        },

        map: {
            '*': {
                'jquery.ui.widget': 'jqueryui'
            }
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
            iframeresizer: {
                deps: ['jquery']
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
        'js/models/SessionTimeout',
        'js/util/SessionRefresherUtil',
        'icanhaz',
        'js/HandlebarsHelpers',
        'modelbinder',
        'bootstrap',
        'templateConfig'
    ], function ($, Backbone, Marionette, Application, ModuleView, AlertsModel, AlertsView, Properties) {

        var app = Application.App;

        // setup the area that the modules will load into and asynchronously require in each module
        // so that it can render itself into the area that was just constructed for it
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

        // setup alert banners
        app.addInitializer(function () {
            var alerts = new Backbone.Collection([]);

            var backendAlerts = new AlertsModel.BackendAlerts();
            backendAlerts.fetch();
            alerts = backendAlerts;

            $(document).ajaxError(function (_, jqxhr) {
                if (jqxhr.status === 401 || jqxhr.status === 403) {
                    var sessionTimeoutAlert = AlertsModel.Jolokia({'stacktrace': 'Forbidden'});
                    // do not show any other alerts if session timeout
                    alerts = new Backbone.Collection([sessionTimeoutAlert]);
                }
            });

            var AlertsCollectionView = Marionette.CollectionView.extend({
                itemView: AlertsView.View,
                comparator: function (model) {
                    return model.get('priority');
                }
            });

            Application.App.alertsRegion.show(new AlertsCollectionView({
                collection: alerts
            }));
        });

        // setup the footer
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

        // once the application has been initialized (i.e. all initializers have completed), start up
        // Backbone.history
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

        // actually start up the application
        app.start();
    });
}());