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

        // locally vendored
        cometd: 'lib/cometd/org/cometd',
        jquerycometd: 'lib/cometd/jquery/jquery.cometd',
        backbonecometd: 'lib/backbone-cometd/backbone.cometd.extension',
        datepickerOverride: 'lib/jquery/js/plugin/jquery-ui-datepicker-4digitYearOverride-addon',
        drawHelper: 'lib/cesium-drawhelper/DrawHelper',

        bootstrap: '/webapp/libs/bootstrap/3.2.0/dist/js/bootstrap.min',
        bootstrapselect: '/webapp/libs/bootstrap-select/1.6.4/dist/js/bootstrap-select.min',

        moment: '/webapp/libs/moment/2.5.1/min/moment.min',
        perfectscrollbar: '/webapp/libs/perfect-scrollbar/0.5.7/min/perfect-scrollbar.min',
        spin: '/webapp/libs/spin.js/1.3.3/spin',
        q: '/webapp/libs/q/1.0.1/q',
        spectrum: '/webapp/libs/spectrum/1.6.0/spectrum',

        // backbone
        backbone: '/webapp/libs/backbone/1.1.2/backbone',
        backboneassociations: '/webapp/libs/backbone-associations/0.6.2/backbone-associations-min',
        backboneundo: '/webapp/libs/Backbone.Undo/0.2.5/Backbone.Undo',
        poller: '/webapp/libs/backbone-poller/1.1.3/backbone.poller',
        underscore: '/webapp/libs/lodash/3.7.0/lodash.min',
        marionette: '/webapp/libs/marionette/2.4.1/lib/backbone.marionette.min',
        'Backbone.ModelBinder': '/webapp/libs/backbone.modelbinder/1.1.0/Backbone.ModelBinder.min',
        collectionbinder: '/webapp/libs/backbone.modelbinder/1.1.0/Backbone.CollectionBinder.min',

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
        jquery: '/webapp/libs/jquery/1.12.4/dist/jquery.min',
        jsCookie: '/webapp/libs/js-cookie/2.1.1/src/js.cookie',
        jqueryuiCore: '/webapp/libs/jquery-ui/1.10.4/ui/minified/jquery.ui.core.min',
        datepicker: '/webapp/libs/jquery-ui/1.10.4/ui/minified/jquery.ui.datepicker.min',
        progressbar: '/webapp/libs/jquery-ui/1.10.4/ui/minified/jquery.ui.progressbar.min',
        slider: '/webapp/libs/jquery-ui/1.10.4/ui/minified/jquery.ui.slider.min',
        mouse: '/webapp/libs/jquery-ui/1.10.4/ui/minified/jquery.ui.mouse.min',
        datepickerAddon: '/webapp/libs/jqueryui-timepicker-addon/1.4.5/src/jquery-ui-timepicker-addon',
        purl: '/webapp/libs/purl/2.3.1/purl',
        multiselect: '/webapp/libs/jquery-ui-multiselect-widget/1.14/src/jquery.multiselect',
        multiselectfilter: '/webapp/libs/jquery-ui-multiselect-widget/1.14/src/jquery.multiselect.filter',
        "jquery.ui.widget": '/webapp/libs/jquery-ui/1.10.4/ui/minified/jquery.ui.widget.min',
        fileupload: '/webapp/libs/jquery-file-upload/9.5.7/js/jquery.fileupload',
        jquerySortable: '/webapp/libs/jquery-ui/1.10.4/ui/minified/jquery.ui.sortable.min',

        // handlebars
        handlebars: '/webapp/libs/handlebars/2.0.0/handlebars.min',
        icanhaz: 'js/ich',

        // require plugins
        text: '/webapp/libs/requirejs-plugins/1.0.2/lib/text',
        css: '/webapp/libs/require-css/0.1.5/css.min',

        // pnotify
        pnotify: '/webapp/libs/pnotify/1.3.1/jquery.pnotify.min',

        // map
        cesium: '/webapp/libs/cesiumjs/1.17.0/Cesium/Cesium',
        openlayers: '/webapp/libs/openlayers3/3.16.0/build/ol',
        usngs: '/webapp/libs/usng.js/0.2.2/usng',

        wellknown: '/webapp/libs/wellknown/0.3.0/wellknown'
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
