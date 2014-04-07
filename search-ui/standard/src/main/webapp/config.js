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

            // application
            application: 'js/application',
            cometdinit: 'js/cometd',
            direction: 'js/direction',
            webglcheck : 'js/webglcheck',
            maptype : 'js/maptype',
            spinnerConfig : 'js/spinnerConfig',
            wreqr: 'js/wreqr',
            properties: 'properties',

            // jquery
            jquery: 'lib/jquery/jquery.min',
            jqueryuiCore: 'lib/jquery-ui/ui/minified/jquery.ui.core.min',
            jqueryuiWidget: 'lib/jquery-ui/ui/minified/jquery.ui.widget.min',
            datepicker: 'lib/jquery-ui/ui/minified/jquery.ui.datepicker.min',
            progressbar: 'lib/jquery-ui/ui/minified/jquery.ui.progressbar.min',
            datepickerOverride: 'lib/jquery/js/plugin/jquery-ui-datepicker-4digitYearOverride-addon',
            datepickerAddon: 'lib/jqueryui-timepicker-addon/src/jquery-ui-timepicker-addon',
            purl: 'lib/purl/purl',
            multiselect: 'lib/multiselect/src/jquery.multiselect',
            multiselectfilter: 'lib/multiselect/src/jquery.multiselect.filter',

            // handlebars
            handlebars: 'lib/handlebars/handlebars.min',
            icanhaz: 'lib/icanhandlebarz/ICanHandlebarz',

            // require plugins
            text: 'lib/requirejs-plugins/lib/text',
            css: 'lib/require-css/css.min',
            
            // pnotify
            pnotify: 'lib/pnotify/jquery.pnotify.min'
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

            datepicker: ['jquery', 'jqueryuiCore'],
            datepickerOverride: ['datepicker'],
            datepickerAddon: ['datepicker'],
            progressbar: ['jquery', 'jqueryuiCore', 'jqueryuiWidget'],
            multiselect: ['jquery', 'jqueryuiWidget'],
            multiselectfilter: ['jquery', 'multiselect'],

            perfectscrollbar: ['jquery']

        },

        waitSeconds: 200
    });

}());