/*global requirejs*/
(function () {
    'use strict';

    requirejs.config({

        paths : {

            // backbone
            backbone : 'core/assets/js/components/backbone/backbone',
            backbonerelational : 'core/assets/js/components/backbone-relational/backbone-relational',
            underscore : 'core/assets/js/components/lodash/dist/lodash.underscore',

            // jquery
            jquery : 'core/assets/js/components/jquery/jquery',
            bootstrap : 'core/assets/js/components/bootstrap/docs/assets/js/bootstrap',
            jqueryui : 'core/assets/js/components/jquery-ui/ui/jquery-ui',

            icanhaz : 'core/assets/js/libs/icanhaz',
            moment : 'core/assets/js/components/moment/moment',
            q : 'core/assets/js/components/q/q',

            // require plugins
            text : 'core/assets/js/components/requirejs-plugins/lib/text',

            properties: './oviz/properties',

            // templates
            templates : 'oviz/assets/templates',

            aviture : 'core/aviture'

        },


        packages : [
            { name: 'cesium',      location: 'core/assets/js/components/cesium/Source' }
        ],

        shim :  {

            backbone : {
                deps: ['underscore', 'jquery'],
                exports: 'Backbone'
            },

            backbonerelational:  ['backbone'],

            underscore: {
                exports: '_'
            },


            icanhaz: {
                deps: ['underscore', 'backbone', 'jquery'],
                exports: 'ich'
            },

            moment : {
                exports : 'moment'
            },

            jqueryui: ['jquery'],
            bootstrap: ['jqueryui']


        },

        waitSeconds : 15
    });

}());