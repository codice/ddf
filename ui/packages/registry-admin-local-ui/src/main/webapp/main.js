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

            bootstrap: 'bootstrap/3.3.7/dist/js/bootstrap.min',
            q: 'q/1.4.1/q',

            // backbone
            backbone: 'backbone/1.1.2/backbone',
            backboneassociation: 'backbone-associations/0.6.2/backbone-associations',
            underscore: 'underscore/1.8.3/underscore-min',
            marionette: 'marionette/1.8.8/lib/backbone.marionette',
            modelbinder: 'backbone.modelbinder/1.1.0/Backbone.ModelBinder',
            collectionbinder: 'backbone.modelbinder/1.1.0/Backbone.CollectionBinder',
            poller: 'backbone-poller/1.1.3/backbone.poller',
            iframeresizer: 'iframe-resizer/2.6.2/js/iframeResizer.min',

            // jquery
            jquery: 'jquery/3.2.1/dist/jquery.min',
            jqueryui: 'jquery-ui/1.12.1/jquery-ui.min',
            multiselect: 'bootstrap-multiselect/0.9.3/js/bootstrap-multiselect',
            perfectscrollbar: 'perfect-scrollbar/0.7.0/js/perfect-scrollbar.jquery.min',

            // handlebars
            handlebars: 'handlebars/4.0.10/handlebars.min',
            icanhaz: 'js/ich',

            // require plugins
            text: 'requirejs-plugins/1.0.3/lib/text',
            css: 'require-css/0.1.10/css',

            // default admin ui
            app: 'js/application',

            moment: 'moment/2.20.1/min/moment.min'
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
            backboneassociation: ['backbone'],
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

            moment: {
                exports: 'moment'
            },

            perfectscrollbar: ['jquery'],

            multiselect: ['jquery'],

            jqueryui: ['jquery'],
            bootstrap: ['jqueryui']

        },

        waitSeconds: 200
    });


    require([
        'jquery',
        'backbone',
        'marionette',
        'icanhaz',
        'js/application',
        'js/HandlebarsHelpers',
        'modelbinder',
        'bootstrap'
    ], function ($, Backbone, Marionette, ich, Application) {


        var app = Application.App;
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
                        // no op
                    }
                };
            }
        }

        // Actually start up the application.
        app.start();

        require(['js/module'], function () {

        });

    });
}());