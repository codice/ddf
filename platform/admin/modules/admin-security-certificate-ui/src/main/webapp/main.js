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

        bootstrap: 'bootstrap/3.2.0/dist/js/bootstrap.min',
        bootstrapselect: 'bootstrap-select/1.6.4/dist/js/bootstrap-select.min',

        moment: 'moment/2.5.1/min/moment.min',
        perfectscrollbar: 'perfect-scrollbar/0.5.7/min/perfect-scrollbar.min',
        q: 'q/1.0.1/q',

        // backbone
        backbone: 'backbone/1.1.2/backbone',
        backboneassociations: 'backbone-associations/0.6.2/backbone-associations-min',
        underscore: 'lodash/3.7.0/lodash.min',
        marionette: 'marionette/2.4.1/lib/backbone.marionette.min',
        // TODO test combining
        modelbinder: 'backbone.modelbinder/1.1.0/Backbone.ModelBinder.min',
        collectionbinder: 'backbone.modelbinder/1.1.0/Backbone.CollectionBinder.min',

        // application
        application: 'js/application',
        properties: 'properties',

        // jquery
        jquery: 'jquery/1.12.4/dist/jquery.min',
        multiselect: 'jquery-ui-multiselect-widget/1.14/src/jquery.multiselect',
        multiselectfilter: 'lib/multiselect/src/jquery.multiselect.filter',
        "jquery.ui.widget": 'jquery-ui/1.10.4/ui/minified/jquery.ui.widget.min',
        fileupload: 'jquery-file-upload/9.5.7/js/jquery.fileupload',

        // handlebars
        handlebars: 'handlebars/2.0.0/handlebars.min',
        icanhaz: 'js/ich',

        // require plugins
        text: 'requirejs-plugins/1.0.2/lib/text',
        css: 'require-css/0.1.5/css.min',
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
