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

(function () {
    'use strict';

    require.config({

        paths: {

            moment: '../../admin/metrics/lib/moment/moment',

            bootstrap: '../../admin/metrics/lib/components-bootstrap/js/bootstrap.min',
            spin: '../../admin/metrics/lib/spin.js/spin',
            q: '../../admin/metrics/lib/q/q',

            // backbone
            backbone: '../../admin/metrics/lib/components-backbone/backbone-min',
            backboneassociation: '../../admin/metrics/lib/backbone-associations/backbone-associations-min',
            underscore: '../../admin/metrics/lib/lodash/dist/lodash.underscore.min',
            marionette: '../../admin/metrics/lib/marionette/lib/backbone.marionette.min',
            // TODO test combining
            modelbinder: '../../admin/metrics/lib/backbone.modelbinder/Backbone.ModelBinder.min',
            collectionbinder: '../../admin/metrics/lib/backbone.modelbinder/Backbone.CollectionBinder.min',
            poller: '../../admin/metrics/lib/backbone-poller/backbone.poller',
            iframeresizer: '../../admin/metrics/lib/iframe-resizer/js/iframeResizer.min',

            // ddf
            spinnerConfig: 'js/spinnerConfig',

            // jquery
            jquery: '../../admin/metrics/lib/jquery/jquery.min',
            jqueryui: '../../admin/metrics/lib/jquery-ui/ui/minified/jquery-ui.min',
            'jquery.ui.widget': '../../admin/metrics/lib/jquery-ui/ui/minified/jquery.ui.widget.min',
            multiselect: '../../admin/metrics/lib/bootstrap-multiselect/js/bootstrap-multiselect',
            perfectscrollbar: '../../admin/metrics/lib/perfect-scrollbar/min/perfect-scrollbar-0.4.8.with-mousewheel.min',
            fileupload: '../../admin/metrics/lib/jquery-file-upload/js/jquery.fileupload',
            fileuploadiframe: '../../admin/metrics/lib/jquery-file-upload/js/jquery.iframe-transport',

            // handlebars
            handlebars: '../../admin/metrics/lib/handlebars/handlebars.min',
            icanhaz: '../../admin/metrics/lib/icanhandlebarz/ICanHandlebarz',

            // require plugins
            text: '../../admin/metrics/lib/requirejs-plugins/lib/text',
            css: '../../admin/metrics/lib/require-css/css',

            // default admin ui
            app: '../../admin/metrics/js/application',

            // datatables
            datatables: '../../admin/metrics/lib/datatables/media/js/jquery.dataTables'
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
                deps: ['modelbinder']
            },
            poller: {
                deps: ['underscore', 'backbone']
            },
            backbonerelational: ['backbone'],
            backboneassociation: ['backbone'],
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
                deps: ['handlebars', 'jquery'],
                exports: 'ich'
            },

            perfectscrollbar: ['jquery'],

            multiselect: ['jquery', 'jquery.ui.widget'],
            fileupload: ['jquery', 'jquery.ui.widget'],

            jqueryui: ['jquery'],
            bootstrap: ['jqueryui']

        },

        waitSeconds: 200
    });


    require([
        'jquery',
        'backbone',
        'marionette',
        'icanhaz',
        'js/application',
        'modelbinder',
        'bootstrap'
    ], function ($, Backbone, Marionette, ich, Application) {


        var app = Application.App;
        // Once the application has been initialized (i.e. all initializers have completed), start up
        // Backbone.history.
        app.on('initialize:after', function () {
            Backbone.history.start();
            //bootstrap call for tabs
            $('tabs').tab();
        });

        if (window) {
            // make ddf object available on window.  Makes debugging in chrome console much easier
            window.app = app;
            if (!window.console) {
                window.console = {
                    log: function () {
                        // no op
                    }
                };
            }
        }

        // Actually start up the application.
        app.start();

        require(['js/module'], function () {

        });

    });
}());