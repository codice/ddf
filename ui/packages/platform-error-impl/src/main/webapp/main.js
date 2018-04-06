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

        baseUrl: "/webjars",

        paths: {
            q: 'q/1.4.1/q',

            // backbone
            backbone: 'backbone/1.1.2/backbone',
            underscore: 'underscore/1.8.3/underscore-min',
            marionette: 'marionette/1.8.8/lib/backbone.marionette.min',

            // jquery
            jquery: 'jquery/3.2.1/dist/jquery.min',

            // handlebars
            handlebars: 'handlebars/2.0.0/handlebars.min',
            icanhaz: '../error/js/ich',
            handlebarsHelpers: '../error/js/HandlebarsHelpers',

            // require plugins
            text: 'requirejs-plugins/1.0.3/lib/text',

            // default login ui
            app: '../error/js/application'
        },


        shim: {

            backbone: {
                deps: ['underscore', 'jquery'],
                exports: 'Backbone'
            },
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
                deps: ['jquery', 'handlebars'],
                exports: 'ich'
            },
            bootstrap: {
                deps: ['jquery']
            }

        },

        waitSeconds: 200
    });


    require([
        'backbone',
        'marionette',
        'icanhaz',
        'app',
        'handlebarsHelpers'

    ], function (Backbone, Marionette, ich, Application) {

        var app = Application.App;

        Marionette.Renderer.render = function (template, data) {
            if (!template) {
                return '';
            }
            return ich[template](data);
        };

        if (window) {
            // make ddf object available on window.  Makes debugging in chrome console much easier
            window.app = app;
        }

        // Actually start up the application.
        app.start();
    });

}());