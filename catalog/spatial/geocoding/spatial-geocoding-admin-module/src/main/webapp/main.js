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
/*global require */
/*jslint nomen:false, -W064 */
require.config({
    paths: {

        bootstrap: '/webapp/libs/bootstrap/3.2.0/dist/js/bootstrap.min',
        moment: '/webapp/libs/moment/2.5.1/min/moment.min',

        // backbone
        backbone: '/webapp/libs/backbone/1.1.2/backbone',

        underscore: '/webapp/libs/lodash/2.4.1/dist/lodash.underscore.min',

        'backbone.marionette': '/webapp/libs/marionette/2.4.1/lib/backbone.marionette.min',

        modelbinder: '/webapp/libs/backbone.modelbinder/1.1.0/Backbone.ModelBinder',

        // application
        application: 'js/application',

        // jquery
        jquery: '/webapp/libs/jquery/1.12.4/dist/jquery.min',
        jqueryuiCore: '/webapp/libs/jquery-ui/1.10.4/ui/minified/jquery.ui.core.min',
        "jquery.ui.widget": '/webapp/libs/jquery-ui/1.10.4/ui/minified/jquery.ui.widget.min',
        fileupload: '/webapp/libs/jquery-file-upload/9.5.7/js/jquery.fileupload',

        // handlebars
        handlebars: '/webapp/libs/handlebars/2.0.0/handlebars.min',
        icanhaz: 'js/ich',

        // require plugins
        text: '/webapp/libs/requirejs-plugins/1.0.2/lib/text',
        css: '/webapp/libs/require-css/0.1.5/css.min'


    },

    shim: {

        backbone: {
            deps: ['underscore', 'jquery'],
            exports: 'Backbone'
        },

        modelbinder: {
            deps: ['underscore', 'jquery', 'backbone']
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

        moment: {
            exports: 'moment'
        },

        jqueryuiCore: ['jquery'],

        fileupload: ['jquery', 'jquery.ui.widget'],

        bootstrap: ['jquery']
    },

    waitSeconds: 0
});

require.onError = function (err) {
    if (typeof console !== 'undefined') {
        console.error("RequireJS failed to load a module", err);
    }
};

require(['jquery',
        'backbone',
        'backbone.marionette',
        'application',
        'icanhaz',
        'js/HandlebarsHelpers',
        'modelbinder'
        ],
    function ($, Backbone, Marionette, Application, ich) {
        'use strict';
        var app = Application.App;
        // Start up backbone.history.
        app.on('initialize:after', function () {
            Backbone.history.start();
            //bootstrap call for tabs
            $('tabs').tab();
        });

        Marionette.Renderer.render = function (template, data) {
            if(!template) {
                return '';
            }
            return ich[template](data);
        };

        // Actually start up the application.
        app.start();
    });
