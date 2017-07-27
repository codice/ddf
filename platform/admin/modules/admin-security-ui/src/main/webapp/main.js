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

        bootstrap: 'bootstrap/3.3.7/dist/js/bootstrap.min',
        bootstrapselect: 'bootstrap-select/1.6.4/dist/js/bootstrap-select.min',

        perfectscrollbar: 'perfect-scrollbar/0.7.0/js/perfect-scrollbar.jquery.min',

        // backbone
        backbone: 'backbone/1.1.2/backbone',
        backboneassociations: 'backbone-associations/0.6.2/backbone-associations-min',
        backboneundo: 'Backbone.Undo/0.2.5/Backbone.Undo',
        poller: 'backbone-poller/1.1.3/backbone.poller',
        underscore: 'underscore/1.8.3/underscore-min',
        marionette: 'marionette/2.4.1/lib/backbone.marionette.min',
        // TODO test combining
        modelbinder: 'backbone.modelbinder/1.1.0/Backbone.ModelBinder.min',
        collectionbinder: 'backbone.modelbinder/1.1.0/Backbone.CollectionBinder.min',

        // application
        application: 'js/application',

        // jquery
        jquery: 'jquery/3.2.1/dist/jquery.min',
        jqueryuiCore: 'jquery-ui/1.12.1/ui/minified/jquery.ui.core.min',
        "jquery.ui.widget": 'jquery-ui/1.12.1/ui/minified/jquery.ui.widget.min',

        // handlebars
        handlebars: 'handlebars/2.0.0/handlebars.min',
        icanhaz: 'js/ich',

        // require plugins
        text: 'requirejs-plugins/1.0.3/lib/text',
        css: 'require-css/0.1.10/css.min',
    },
    map: {
        '*': {
            'Backbone.ModelBinder': 'modelbinder',
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
        backboneassociations: ['backbone'],
        backbonecometd: ['underscore', 'jquery', 'backbone', 'cometdinit'],
        marionette: {
            deps: ['jquery', 'underscore', 'backbone'],
            exports: 'Marionette'
        },

        perfectscrollbar: ['jquery'],

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

        jqueryuiCore: ['jquery'],

        spectrum: ['jquery'],

        bootstrap: ['jquery'],

        bootstrapselect: ['bootstrap']
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
        'marionette',
        'application',
        'icanhaz'],
    function ($, Backbone, Marionette, app, ich) {
        'use strict';

        //in here we drop in any top level patches, etc.

        Marionette.Renderer.render = function (template, data) {
            if(!template){return '';}
            return ich[template](data);
        };

        // Actually start up the application.
        app.App.start({});
    });
