/*global requirejs*/
(function () {
    'use strict';

    requirejs.config({

        paths: {

            // backbone
            backbone: 'lib/backbone/backbone',
            backbonerelational: 'lib/backbone-relational/backbone-relational',
            backbonecometd: 'lib/backbone-cometd/backbone.cometd.extension',
            underscore: 'lib/lodash/dist/lodash.underscore',
            marionette: 'lib/marionette/lib/backbone.marionette',
            // TODO test combining
            modelbinder: 'lib/modelbinder/Backbone.ModelBinder.min',
            collectionbinder: 'lib/modelbinder/Backbone.CollectionBinder.min',

            // jquery
            jquery: 'lib/jquery/js/jquery-1.10.2.min',
            bootstrap: 'lib/bootstrap-2.3.1/js/bootstrap.min',
            partialaffix: 'lib/bootstrap-extensions/js/partial-affix',
            datepickerOverride: 'lib/jquery/js/plugin/jquery-ui-datepicker-4digitYearOverride-addon',
            datepickerAddon: 'lib/jquery/js/plugin/jquery-ui-timepicker-addon',
            purl: 'lib/jquery/js/plugin/purl',
            jqueryui: 'lib/jquery/js/jquery-ui-1.10.3.min',
            perfectscrollbar: 'lib/perfect-scrollbar/min/perfect-scrollbar.with-mousewheel.min',
            spin: 'lib/spin/spin',
            // handlebars
            handlebars: 'lib/handlebars/handlebars-v1.1.2',
            icanhaz: 'lib/icanhaz/ICanHandlebarz',

            moment: 'lib/moment/moment',
            q: 'lib/q/q',

            ddf: 'js/ddf',
            cometdinit: 'js/cometd',
            direction: 'js/direction',
            webglcheck : 'js/webglcheck',
            maptype : 'js/maptype',
            spinnerConfig : 'js/spinnerConfig',
            // require plugins
            text: 'lib/requirejs-plugins/lib/text',
            cesium: 'lib/cesium/Cesium',
            properties: 'properties',
            cometd: 'lib/cometd/org/cometd',
            jquerycometd: 'lib/cometd/jquery/jquery.cometd'
        },


        shim: {

            jquerycometd: {
                deps: ['jquery', 'cometd']
            },

            backbone: {
                deps: ['underscore', 'jquery'],
                exports: 'Backbone'
            },

            cesium: {
                exports: 'Cesium'
            },

            backbonerelational: ['backbone'],
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
                deps: ['handlebars'],
                exports: 'ich'
            },

            moment: {
                exports: 'moment'
            },

            jqueryui: ['jquery'],
            bootstrap: ['jqueryui'],
            partialaffix: ['bootstrap'],

            perfectscrollbar: ['jquery'],
            datepickerOverride: ['jquery'],
            datepickerAddon: ['jquery']

        },

        waitSeconds: 15
    });

}());