/*global requirejs*/
(function () {
    'use strict';

    requirejs.config({

        paths : {

            // backbone
            backbone : 'lib/backbone/backbone',
            backbonerelational : 'lib/backbone-relational/backbone-relational',
            underscore : 'lib/lodash/dist/lodash.underscore',
            marionette : 'lib/marionette/lib/backbone.marionette',
            // TODO test combining
            modelbinder : 'lib/modelbinder/Backbone.ModelBinder.min',
            collectionbinder : 'lib/modelbinder/Backbone.CollectionBinder.min',

            // jquery
            jquery : 'lib/jquery/js/jquery-1.9.1.min',
            bootstrap : 'lib/bootstrap-2.3.1/js/bootstrap.min',
            partialaffix : 'lib/bootstrap-extensions/js/partial-affix',
            datepickerOverride : 'lib/jquery/js/plugin/jquery-ui-datepicker-4digitYearOverride-addon',
            datepickerAddon : 'lib/jquery/js/plugin/jquery-ui-timepicker-addon',
            purl : 'lib/jquery/js/plugin/purl',
            jqueryui : 'lib/jquery/js/jquery-ui-1.9.1.custom.min',
            perfectscrollbar : 'lib/perfect-scrollbar/min/perfect-scrollbar.with-mousewheel.min',
            spin : 'lib/spin/spin',
            // handlebars
            handlebars : 'lib/handlebars/handlebars-v1.1.2',
            icanhaz : 'lib/icanhaz/ICanHandlebarz',

            moment : 'lib/moment/moment',
            q : 'lib/q/q',

            ddf : 'js/ddf',
            // require plugins
            text : 'lib/requirejs-plugins/lib/text',
            cesium : 'lib/cesium/Cesium'

        },



        shim :  {

            backbone : {
                deps: ['underscore', 'jquery'],
                exports: 'Backbone'
            },

            cesium : {
                exports : 'Cesium'
            },

            backbonerelational:  ['backbone'],
            marionette : {
                deps : ['jquery', 'underscore', 'backbone'],
                exports : 'Marionette'
            },
            underscore: {
                exports: '_'
            },
            icanhaz: {
                deps: ['handlebars'],
                exports: 'ich'
            },

            moment : {
                exports : 'moment'
            },

            jqueryui: ['jquery'],
            bootstrap: ['jqueryui'],
            partialaffix : ['bootstrap'],
            purl : ['jquery'],

            perfectscrollbar : ['jquery'],

            datepickerOverride : ['jquery'],
            datepickerAddon : ['jquery']

        },

        waitSeconds : 15
    });

}());