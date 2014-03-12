/*global requirejs*/
(function () {
    'use strict';

    requirejs.config({

        paths: {

            bootstrap: 'lib/components-bootstrap/js/bootstrap.min',
            cesium: 'lib/cesium/Cesium/Cesium',
            cometd: 'lib/cometd/org/cometd',
            jquerycometd: 'lib/cometd/jquery/jquery.cometd',
            moment: 'lib/moment/min/moment.min',
            perfectscrollbar: 'lib/perfect-scrollbar/min/perfect-scrollbar.with-mousewheel.min',
            spin: 'lib/spin.js/spin',
            q: 'lib/q/q',

            // backbone
            backbone: 'lib/components-backbone/backbone-min',
            backbonerelational: 'lib/backbone-relational/backbone-relational',
            backbonecometd: 'lib/backbone-cometd/backbone.cometd.extension',
            underscore: 'lib/lodash/dist/lodash.underscore.min',
            marionette: 'lib/marionette/lib/backbone.marionette.min',
            // TODO test combining
            modelbinder: 'lib/backbone.modelbinder/Backbone.ModelBinder.min',
            collectionbinder: 'lib/backbone.modelbinder/Backbone.CollectionBinder.min',

            // ddf
            ddf: 'js/ddf',
            cometdinit: 'js/cometd',
            direction: 'js/direction',
            webglcheck : 'js/webglcheck',
            maptype : 'js/maptype',
            spinnerConfig : 'js/spinnerConfig',
            properties: 'properties',

            // jquery
            jquery: 'lib/jquery/jquery.min',
            jqueryui: 'lib/jquery-ui/ui/minified/jquery-ui.min',
            datepickerOverride: 'lib/jquery/js/plugin/jquery-ui-datepicker-4digitYearOverride-addon',
            datepickerAddon: 'lib/jquery/js/plugin/jquery-ui-timepicker-addon',
            purl: 'lib/purl/purl',
            multiselect: 'lib/multiselect/src/jquery.multiselect',
            multiselectfilter: 'lib/multiselect/src/jquery.multiselect.filter',

            // handlebars
            handlebars: 'lib/handlebars/handlebars.min',
            icanhaz: 'lib/icanhandlebarz/ICanHandlebarz',

            // require plugins
            text: 'lib/requirejs-plugins/lib/text'
        },


        shim: {

            jquerycometd: {
                deps: ['jquery', 'cometd']
            },

            multiselect: {
                deps: ['jquery']
            },

            multiselectfilter: {
                deps: ['jquery']
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

            perfectscrollbar: ['jquery'],
            datepickerOverride: ['jquery'],
            datepickerAddon: ['jquery']

        },

        waitSeconds: 200
    });

}());