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
/*global requirejs*/
(function () {
    'use strict';

    requirejs.config({

        paths: {

            bootstrap: 'lib/components-bootstrap/js/bootstrap.min',
            cesium: 'lib/cesium/Cesium',
            openlayers: 'lib/openlayers3/build/ol',
            cometd: 'lib/cometd/org/cometd',
            jquerycometd: 'lib/cometd/jquery/jquery.cometd',
            moment: 'lib/moment/min/moment.min',
            perfectscrollbar: 'lib/perfect-scrollbar/min/perfect-scrollbar.with-mousewheel.min',
            spin: 'lib/spin.js/spin',
            q: 'lib/q/q',
            strapdown: 'lib/strapdown/v/0.2',

            // backbone
            backbone: 'lib/components-backbone/backbone-min',
            backboneassociations: 'lib/backbone-associations/backbone-associations-min',
            backbonecometd: 'lib/backbone-cometd/backbone.cometd.extension',
            poller: 'lib/backbone-poller/backbone.poller',
            underscore: 'lib/lodash/dist/lodash.underscore.min',
            lodash: 'lib/lodash/dist/lodash.min',
            marionette: 'lib/marionette/lib/backbone.marionette.min',
            // TODO test combining
            modelbinder: 'lib/backbone.modelbinder/Backbone.ModelBinder.min',
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
            user: 'js/user',
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
            pnotify: 'lib/pnotify/jquery.pnotify.min',

            // map tools
            usngs: 'lib/usng/usng'
        },


        shim: {

            collectionbinder: {
                deps: ['modelbinder']
            },

            jquerycometd: {
                deps: ['jquery', 'cometd']
            },

            backbone: {
                deps: ['underscore', 'jquery'],
                exports: 'Backbone'
            },

            poller: {
                deps: ['backbone']
            },

            cesium: {
                exports: 'Cesium'
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

            perfectscrollbar: ['jquery'],

            purl: ['jquery'],

            bootstrap: ['jquery'],

            openlayers: {
                exports: 'ol'
            }

        },

        waitSeconds: 200
    });

}());
