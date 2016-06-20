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
require.config({
    paths: {

        bootstrap: 'lib/bootstrap/dist/js/bootstrap.min',
        bootstrapselect: 'lib/bootstrap-select/dist/js/bootstrap-select.min',

        cometd: 'lib/cometd/org/cometd',
        jquerycometd: 'lib/cometd/jquery/jquery.cometd',
        moment: 'lib/moment/min/moment.min',
        perfectscrollbar: 'lib/perfect-scrollbar/min/perfect-scrollbar.min',
        spin: 'lib/spin.js/spin',
        q: 'lib/q/q',
        spectrum: 'lib/spectrum/spectrum',

        // backbone
        backbone: 'lib/backbone/backbone',
        backboneassociations: 'lib/backbone-associations/backbone-associations-min',
        backbonecometd: 'lib/backbone-cometd/backbone.cometd.extension',
        backboneundo: 'lib/Backbone.Undo/Backbone.Undo',
        poller: 'lib/backbone-poller/backbone.poller',
        underscore: 'lib/lodash/lodash.min',
        marionette: 'lib/marionette/lib/backbone.marionette.min',
        'Backbone.ModelBinder': 'lib/backbone.modelbinder/Backbone.ModelBinder.min',
        collectionbinder: 'lib/backbone.modelbinder/Backbone.CollectionBinder.min',

        // application
        application: 'js/application',
        cometdinit: 'js/cometd',
        direction: 'js/direction',
        webglcheck : 'js/webglcheck',
        twodcheck : 'js/2dmapcheck',
        maptype : 'js/maptype',
        spinnerConfig : 'js/spinnerConfig',
        wreqr: 'js/wreqr',
        properties: 'properties',

        // jquery
        jquery: 'lib/jquery/dist/jquery.min',
        jsCookie: 'lib/js-cookie/src/js.cookie',
        jqueryuiCore: 'lib/jquery-ui/ui/minified/jquery.ui.core.min',
        datepicker: 'lib/jquery-ui/ui/minified/jquery.ui.datepicker.min',
        progressbar: 'lib/jquery-ui/ui/minified/jquery.ui.progressbar.min',
        slider: 'lib/jquery-ui/ui/minified/jquery.ui.slider.min',
        mouse: 'lib/jquery-ui/ui/minified/jquery.ui.mouse.min',
        datepickerOverride: 'lib/jquery/js/plugin/jquery-ui-datepicker-4digitYearOverride-addon',
        datepickerAddon: 'lib/jqueryui-timepicker-addon/src/jquery-ui-timepicker-addon',
        purl: 'lib/purl/purl',
        multiselect: 'lib/jquery-ui-multiselect-widget/src/jquery.multiselect',
        multiselectfilter: 'lib/jquery-ui-multiselect-widget/src/jquery.multiselect.filter',
        "jquery.ui.widget": 'lib/jquery-ui/ui/minified/jquery.ui.widget.min',
        fileupload: 'lib/jquery-file-upload/js/jquery.fileupload',
        jquerySortable: 'lib/jquery-ui/ui/minified/jquery.ui.sortable.min',

        // handlebars
        handlebars: 'lib/handlebars/handlebars.min',
        icanhaz: 'js/ich',

        // require plugins
        text: 'lib/requirejs-plugins/lib/text',
        css: 'lib/require-css/css.min',

        // pnotify
        pnotify: 'lib/pnotify/jquery.pnotify.min',

        // map
        cesium: 'lib/cesiumjs/Cesium/Cesium',
        drawHelper: 'lib/cesium-drawhelper/DrawHelper',
        openlayers: 'lib/openlayers3/build/ol',
        usngs: 'lib/usng.js/usng',

        wellknown: 'lib/wellknown/wellknown'
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
            deps: ['underscore', 'jquery', 'backbone', 'Backbone.ModelBinder']
        },
        poller: {
            deps: ['underscore', 'backbone']
        },
        backboneassociations: ['backbone'],
        backbonecometd: ['underscore', 'jquery', 'backbone', 'cometdinit'],
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

        jquerycometd: {
            deps: ['jquery', 'cometd']
        },
        jqueryuiCore: ['jquery'],
        jsCookie: ['jquery'],
        mouse: ['jqueryuiCore', 'jquery.ui.widget'],
        slider: ['mouse'],
        datepicker: ['slider'],
        datepickerOverride: ['datepicker'],
        datepickerAddon: ['datepicker'],
        progressbar: ['jquery', 'jqueryuiCore', 'jquery.ui.widget'],
        multiselect: ['jquery', 'jquery.ui.widget'],
        multiselectfilter: ['jquery', 'multiselect'],
        fileupload: ['jquery', 'jquery.ui.widget'],
        jquerySortable: ['jquery', 'jqueryuiCore', 'jquery.ui.widget', 'mouse'],

        perfectscrollbar: ['jquery'],

        purl: ['jquery'],

        spectrum: ['jquery'],

        bootstrap: ['jquery'],

        cesium: {
            exports: 'Cesium'
        },
        drawHelper: {
            deps: ['cesium'],
            exports: 'DrawHelper'
        },
        openlayers: {
            exports: 'ol'
        },

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
        'jquery',
        'backbone',
        'marionette',
        'application',
        'icanhaz',
        'properties',
        'js/HandlebarsHelpers',
        'js/ApplicationHelpers'],
    function (_, $, Backbone, Marionette, app, ich, properties) {
        'use strict';

        // Make lodash compatible with Backbone
        var lodash = _.noConflict();
        _.mixin({
            'debounce': _.debounce || lodash.debounce,
            'defer': _.defer || lodash.defer,
            'pluck': _.pluck || lodash.pluck
        });

        var document = window.document;

        //in here we drop in any top level patches, etc.

        Marionette.Renderer.render = function (template, data) {
            if(!template){return '';}
            return ich[template](data);
        };

        //TODO: this hack here is to fix the issue of the main div not resizing correctly
        //when the header and footer are in place
        //remove this code when the correct way to get the div to resize is discovered
        $(window).resize(function () {
            var height = $('body').height();
            if (properties.ui.header && properties.ui.header !== '') {
                height = height - 20;
            }
            if (properties.ui.footer && properties.ui.footer !== '') {
                height = height - 20;
            }
            $('#content').height(height);
        });

        $(window).trigger('resize');

        $(document).ready(function () {
            document.title = properties.branding;
        });

        // Actually start up the application.
        app.App.start({});
    });
