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

        bootstrap: 'bootstrap/3.2.0/dist/js/bootstrap.min',
        bootstrapselect: 'bootstrap-select/1.6.4/dist/js/bootstrap-select.min',

        cometd: 'lib/cometd/org/cometd',
        jquerycometd: 'lib/cometd/jquery/jquery.cometd',
        moment: 'moment/2.5.1/min/moment.min',
        perfectscrollbar: 'perfect-scrollbar/0.5.7/min/perfect-scrollbar.min',
        spin: 'spin.js/1.3.3/spin',
        q: 'q/1.0.1/q',
        spectrum: 'spectrum/1.6.0/spectrum',

        // backbone
        backbone: 'backbone/1.1.2/backbone',
        backboneassociations: 'backbone-associations/0.6.2/backbone-associations-min',
        backbonecometd: 'lib/backbone-cometd/backbone.cometd.extension',
        backboneundo: 'Backbone.Undo/0.2.5/Backbone.Undo',
        poller: 'backbone-poller/1.1.3/backbone.poller',
        underscore: 'lodash/3.7.0/lodash.min',
        marionette: 'marionette/2.4.1/lib/backbone.marionette.min',
        'Backbone.ModelBinder': 'backbone.modelbinder/1.1.0/Backbone.ModelBinder.min',
        collectionbinder: 'backbone.modelbinder/1.1.0/Backbone.CollectionBinder.min',

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
        jquery: 'jquery/1.12.4/dist/jquery.min',
        jsCookie: 'js-cookie/2.1.1/src/js.cookie',
        jqueryuiCore: 'jquery-ui/1.10.4/ui/minified/jquery.ui.core.min',
        datepicker: 'jquery-ui/1.10.4/ui/minified/jquery.ui.datepicker.min',
        progressbar: 'jquery-ui/1.10.4/ui/minified/jquery.ui.progressbar.min',
        slider: 'jquery-ui/1.10.4/ui/minified/jquery.ui.slider.min',
        mouse: 'jquery-ui/1.10.4/ui/minified/jquery.ui.mouse.min',
        datepickerOverride: 'lib/jquery/js/plugin/jquery-ui-datepicker-4digitYearOverride-addon',
        datepickerAddon: 'jqueryui-timepicker-addon/1.4.5/src/jquery-ui-timepicker-addon',
        purl: 'purl/2.3.1/purl',
        multiselect: 'jquery-ui-multiselect-widget/1.14/src/jquery.multiselect',
        multiselectfilter: 'jquery-ui-multiselect-widget/1.14/src/jquery.multiselect.filter',
        "jquery.ui.widget": 'jquery-ui/1.10.4/ui/minified/jquery.ui.widget.min',
        fileupload: 'jquery-file-upload/9.5.7/js/jquery.fileupload',
        jquerySortable: 'jquery-ui/1.10.4/ui/minified/jquery.ui.sortable.min',

        // handlebars
        handlebars: 'handlebars/2.0.0/handlebars.min',
        icanhaz: 'js/ich',

        // require plugins
        text: 'requirejs-plugins/1.0.2/lib/text',
        css: 'require-css/0.1.5/css.min',

        // pnotify
        pnotify: 'pnotify/1.3.1/jquery.pnotify.min',

        // map
        cesium: 'cesiumjs/1.17.0/Cesium/Cesium',
        drawHelper: 'lib/cesium-drawhelper/DrawHelper',
        openlayers: 'openlayers3/3.16.0/build/ol',
        usngs: 'usng.js/0.2.2/usng',

        wellknown: 'wellknown/0.4.0/wellknown'
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
