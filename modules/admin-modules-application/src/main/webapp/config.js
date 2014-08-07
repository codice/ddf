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
            spin: 'lib/spin.js/spin',
            q: 'lib/q/q',

            // backbone
            backbone: 'lib/components-backbone/backbone-min',
            backbonerelational: 'lib/backbone-relational/backbone-relational',
            underscore: 'lib/lodash/dist/lodash.underscore.min',
            marionette: 'lib/marionette/lib/backbone.marionette.min',
            // TODO test combining
            modelbinder: 'lib/backbone.modelbinder/Backbone.ModelBinder.min',
            collectionbinder: 'lib/backbone.modelbinder/Backbone.CollectionBinder.min',
            poller: 'lib/backbone-poller/backbone.poller',

            // ddf
            spinnerConfig : 'js/spinnerConfig',
            properties: 'properties',

            // jquery
            jquery: 'lib/jquery/jquery.min',
            jqueryui: 'lib/jquery-ui/ui/minified/jquery-ui.min',
            'jquery.ui.widget': 'lib/jquery-ui/ui/minified/jquery.ui.widget.min',
            multiselect: 'lib/bootstrap-multiselect/js/bootstrap-multiselect',
            perfectscrollbar: 'lib/perfect-scrollbar/min/perfect-scrollbar-0.4.8.with-mousewheel.min',
            fileupload: 'lib/jquery-file-upload/js/jquery.fileupload',
            fileuploadiframe: 'lib/jquery-file-upload/js/jquery.iframe-transport',

            // handlebars
            handlebars: 'lib/handlebars/handlebars.min',
            icanhaz: 'lib/icanhandlebarz/ICanHandlebarz',

            // require plugins
            text: 'lib/requirejs-plugins/lib/text'
        },


        shim: {

            backbone: {
                deps: ['underscore', 'jquery'],
                exports: 'Backbone'
            },

            backbonerelational: ['backbone'],

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
                deps: ['handlebars','jquery'],
                exports: 'ich'
            },

            jqueryui: ['jquery'],
            bootstrap: ['jqueryui']

        },

        waitSeconds: 15
    });

}());