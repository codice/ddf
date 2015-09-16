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

        bootstrap: 'lib/components-bootstrap/js/bootstrap.min',
        bootstrapselect: 'lib/bootstrap-select/dist/js/bootstrap-select.min',

        moment: 'lib/moment/min/moment.min',
        perfectscrollbar: 'lib/perfect-scrollbar/min/perfect-scrollbar.min',
        q: 'lib/q/q',

        // backbone
        backbone: 'lib/components-backbone/backbone-min',
        backboneassociations: 'lib/backbone-associations/backbone-associations-min',
        underscore: 'lib/lodash/lodash.min',
        marionette: 'lib/marionette/lib/backbone.marionette.min',
        // TODO test combining
        modelbinder: 'lib/backbone.modelbinder/Backbone.ModelBinder.min',
        collectionbinder: 'lib/backbone.modelbinder/Backbone.CollectionBinder.min',

        // application
        application: 'js/application',
        properties: 'properties',

        // jquery
        jquery: 'lib/jquery/jquery.min',
        multiselect: 'lib/multiselect/src/jquery.multiselect',
        multiselectfilter: 'lib/multiselect/src/jquery.multiselect.filter',
        "jquery.ui.widget": 'lib/jquery-ui/ui/minified/jquery.ui.widget.min',
        fileupload: 'lib/jquery-file-upload/js/jquery.fileupload',

        // handlebars
        handlebars: 'lib/handlebars/handlebars.min',
        icanhaz: 'lib/icanhandlebarz/ICanHandlebarz',

        // require plugins
        text: 'lib/requirejs-plugins/lib/text',
        css: 'lib/require-css/css.min',
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

        datepicker: ['jquery', 'jqueryuiCore'],
        datepickerOverride: ['datepicker'],
        datepickerAddon: ['datepicker'],
        progressbar: ['jquery', 'jqueryuiCore', 'jquery.ui.widget'],
        multiselect: ['jquery', 'jquery.ui.widget'],
        multiselectfilter: ['jquery', 'multiselect'],
        fileupload: ['jquery', 'jquery.ui.widget'],

        perfectscrollbar: ['jquery'],

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

require(['underscore',
        'application',
        'marionette',
        'icanhaz',
        'js/HandlebarsHelpers',
        'bootstrap'],
    function (_, app, Marionette, ich) {
        'use strict';

        // Make lodash compatible with Backbone
        var lodash = _.noConflict();
        _.mixin({
            'debounce': _.debounce || lodash.debounce,
            'defer': _.defer || lodash.defer,
            'pluck': _.pluck || lodash.pluck
        });

        //in here we drop in any top level patches, etc.

        Marionette.Renderer.render = function (template, data) {
            if(!template){return '';}
            return ich[template](data);
        };

        // Actually start up the application.
        app.App.start({});
    });
