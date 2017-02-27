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

            bootstrap: 'bootstrap/3.2.0/dist/js/bootstrap.min',
            q: 'q/1.0.1/q',

            // backbone
            backbone: 'backbone/1.1.2/backbone',
            underscore: 'lodash/2.4.1/dist/lodash.underscore.min',
            marionette: 'marionette/1.8.8/lib/backbone.marionette.min',

            // jquery
            jquery: 'jquery/1.12.4/dist/jquery.min',
            jqueryuiCore: 'jquery-ui/1.10.4/ui/minified/jquery.ui.core.min',

            // purl
            purl: 'purl/2.3.1/purl',

            // handlebars
            handlebars: 'handlebars/2.0.0/handlebars.min',
            icanhaz: 'js/ich',

            // require plugins
            text: 'requirejs-plugins/1.0.2/lib/text',

            // default login ui
            app: 'js/application'
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
            },
            purl: {
                deps: ['jquery']
            }

        },

        waitSeconds: 200
    });


    require([
        'backbone',
        'marionette',
        'icanhaz',
        'js/application',
        'bootstrap'

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