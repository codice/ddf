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

            bootstrap: '../admin/lib/components-bootstrap/js/bootstrap.min',
            spin: '../admin/lib/spin.js/spin',
            q: '../admin/lib/q/q',

            // backbone
            backbone: '../admin/lib/components-backbone/backbone-min',
            backbonerelational: '../admin/lib/backbone-relational/backbone-relational',
            underscore: '../admin/lib/lodash/dist/lodash.underscore.min',
            marionette: '../admin/lib/marionette/lib/backbone.marionette.min',
            // TODO test combining
            modelbinder: '../admin/lib/backbone.modelbinder/Backbone.ModelBinder.min',
            collectionbinder: '../admin/lib/backbone.modelbinder/Backbone.CollectionBinder.min',
            poller: '../admin/lib/backbone-poller/backbone.poller',

            // ddf
            spinnerConfig : 'js/spinnerConfig',

            // jquery
            jquery: '../admin/lib/jquery/jquery.min',
            jqueryui: '../admin/lib/jquery-ui/ui/minified/jquery-ui.min',
            'jquery.ui.widget': '../admin/lib/jquery-ui/ui/minified/jquery.ui.widget.min',
            multiselect: '../admin/lib/bootstrap-multiselect/js/bootstrap-multiselect',
            perfectscrollbar: '../admin/lib/perfect-scrollbar/min/perfect-scrollbar-0.4.8.with-mousewheel.min',
            fileupload: '../admin/lib/jquery-file-upload/js/jquery.fileupload',
            fileuploadiframe: '../admin/lib/jquery-file-upload/js/jquery.iframe-transport',

            // handlebars
            handlebars: '../admin/lib/handlebars/handlebars.min',
            icanhaz: '../admin/lib/icanhandlebarz/ICanHandlebarz',

            // require plugins
            text: '../admin/lib/requirejs-plugins/lib/text',
            css: '../admin/lib/require-css/css',

            // default admin ui
            app: '../admin/js/application',

            // datatables
            datatables: '../admin/lib/datatables/media/js/jquery.dataTables'
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

            perfectscrollbar: ['jquery'],

            multiselect: ['jquery'],

            jqueryui: ['jquery'],
            bootstrap: ['jqueryui']

        },

        waitSeconds: 200
    });

}());