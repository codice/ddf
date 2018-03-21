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

        bootstrap: '/webjars/bootstrap/3.3.7/dist/js/bootstrap.min',
        bootstrapselect: '/webjars/bootstrap-select/1.6.4/dist/js/bootstrap-select.min',

        cometd: 'lib/cometd/org/cometd',
        jquerycometd: 'lib/cometd/jquery/jquery.cometd',
        moment: '/webjars/moment/2.20.1/min/moment.min',
        perfectscrollbar: '/webjars/perfect-scrollbar/0.7.0/js/perfect-scrollbar.jquery.min',
        spin: '/webjars/spin.js/2.3.2/spin',
        q: '/webjars/q/1.4.1/q',
        spectrum: '/webjars/spectrum/1.8.0/spectrum',

        // backbone
        backbone: '/webjars/backbone/1.1.2/backbone',
        backboneassociations: '/webjars/backbone-associations/0.6.2/backbone-associations-min',
        backbonecometd: 'lib/backbone-cometd/backbone.cometd.extension',
        backboneundo: '/webjars/Backbone.Undo/0.2.5/Backbone.Undo',
        poller: '/webjars/backbone-poller/1.1.3/backbone.poller',
        underscore: '/webjars/lodash/3.7.0/lodash.min',
        marionette: '/webjars/marionette/2.4.5/lib/backbone.marionette.min',
        'Backbone.ModelBinder': '/webjars/backbone.modelbinder/1.1.0/Backbone.ModelBinder.min',
        collectionbinder: '/webjars/backbone.modelbinder/1.1.0/Backbone.CollectionBinder.min',

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
        jquery: '/webjars/jquery/3.2.1/dist/jquery.min',
        jsCookie: '/webjars/js-cookie/2.1.4/src/js.cookie',
        jqueryui: '/webjars/jquery-ui/1.12.1/jquery-ui.min',
        datepickerOverride: 'lib/jquery/js/plugin/jquery-ui-datepicker-4digitYearOverride-addon',
        datepickerAddon: '/webjars/jqueryui-timepicker-addon/1.4.5/src/jquery-ui-timepicker-addon',
        purl: '/webjars/purl/2.3.1/purl',
        multiselect: '/webjars/jquery-ui-multiselect-widget/1.14/src/jquery.multiselect',
        multiselectfilter: '/webjars/jquery-ui-multiselect-widget/1.14/src/jquery.multiselect.filter',
        fileupload: '/webjars/jquery-file-upload/9.5.7/js/jquery.fileupload',

        // handlebars
        handlebars: '/webjars/handlebars/2.0.0/handlebars.min',
        icanhaz: 'js/ich',

        // require plugins
        text: '/webjars/requirejs-plugins/1.0.3/lib/text',
        css: '/webjars/require-css/0.1.10/css.min',

        // pnotify
        pnotify: '/webjars/pnotify/1.3.1/jquery.pnotify.min',

        // map
        cesium: '/webjars/cesiumjs/1.22.0/Cesium/Cesium',
        drawHelper: 'lib/cesium-drawhelper/DrawHelper',
        openlayers: '/webjars/openlayers3/3.16.0/build/ol',
        usngs: '/webjars/usng.js/0.2.2/usng',

        wellknown: '/webjars/wellknown/0.4.0/wellknown'
    },
    map: {
        '*': {
            'jquery.ui.widget': 'jqueryui',
            'datepicker': 'jqueryui',
            'progressbar': 'jqueryui',
            'jquerySortable': 'jqueryui'
        },
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
        jqueryui: ['jquery'],
        jsCookie: ['jquery'],
        datepickerOverride: ['jqueryui'],
        datepickerAddon: ['jqueryui'],
        multiselect: ['jqueryui'],
        multiselectfilter: ['jqueryui'],
        fileupload: ['jqueryui'],
        jquerySortable: ['jqueryui'],

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

        // https://github.com/marionettejs/backbone.marionette/issues/3077
        // monkey-patch Marionette for compatibility with jquery 3+.
        // jquery removed the .selector method, which was used by the original
        // implementation here.
        Marionette.Region.prototype.reset = function() {
            this.empty();
            this.el = this.options.el;
            delete this.$el;
            return this;
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
