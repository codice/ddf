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
            q: 'lib/q/q',

            // backbone
            backbone: 'lib/backbone/backbone',
            backboneassociation: 'lib/backbone-associations/backbone-associations',
            underscore: 'lib/lodash/dist/lodash.underscore',
            marionette: 'lib/marionette/lib/backbone.marionette',
            modelbinder: 'lib/backbone.modelbinder/Backbone.ModelBinder',
            collectionbinder: 'lib/backbone.modelbinder/Backbone.CollectionBinder',
            poller: 'lib/backbone-poller/backbone.poller',
            iframeresizer: 'lib/iframe-resizer/js/iframeResizer.min',

            // jquery
            jquery: 'lib/jquery/dist/jquery.min',
            jqueryui: 'lib/jquery-ui/ui/minified/jquery-ui.min',
            'jquery.ui.widget': 'lib/jquery-ui/ui/minified/jquery.ui.widget.min',
            multiselect: 'lib/bootstrap-multiselect/js/bootstrap-multiselect',
            perfectscrollbar: 'lib/perfect-scrollbar/min/perfect-scrollbar-0.4.8.with-mousewheel.min',

            // handlebars
            handlebars: 'lib/handlebars/handlebars.min',
            icanhaz: 'js/ich',

            // require plugins
            text: 'lib/requirejs-plugins/lib/text',
            css: 'lib/require-css/css',

            // default admin ui
            app: 'js/application'
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