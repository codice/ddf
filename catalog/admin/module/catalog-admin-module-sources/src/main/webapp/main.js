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

            bootstrap: 'bootstrap/3.2.0/dist/js/bootstrap.min',
            spin: 'spin.js/1.3.3/spin',
            q: 'q/1.0.1/q',

            // backbone
            backbone: 'backbone/1.1.2/backbone',
            backbonerelational: 'backbone-relational/0.8.8/backbone-relational',
            backboneassociation: 'backbone-associations/0.6.2/backbone-associations-min',
            underscore: 'lodash/2.4.1/dist/lodash.underscore.min',
            marionette: 'marionette/1.8.8/lib/backbone.marionette.min',
            // TODO test combining
            modelbinder: 'backbone.modelbinder/1.1.0/Backbone.ModelBinder.min',
            collectionbinder: 'backbone.modelbinder/1.1.0/Backbone.CollectionBinder.min',
            poller: 'backbone-poller/1.1.3/backbone.poller',
            iframeresizer: 'iframe-resizer/2.6.2/js/iframeResizer.min',

            // jquery
            jquery: 'jquery/1.12.4/dist/jquery.min',
            jqueryui: 'jquery-ui/1.10.4/ui/minified/jquery-ui.min',
            'jquery.ui.widget': 'jquery-ui/1.10.4/ui/minified/jquery.ui.widget.min',
            multiselect: 'bootstrap-multiselect/0.9.3/js/bootstrap-multiselect',
            perfectscrollbar: 'perfect-scrollbar/0.7.0/js/perfect-scrollbar.jquery.min',
            fileupload: 'jquery-file-upload/9.5.7/js/jquery.fileupload',
            fileuploadiframe: 'jquery-file-upload/9.5.7/js/jquery.iframe-transport',

            // handlebars
            handlebars: 'handlebars/2.0.0/handlebars.min',
            icanhaz: 'js/ich',

            // require plugins
            text: 'requirejs-plugins/1.0.3/lib/text',
            css: 'require-css/0.1.10/css',

            // datatables

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

            multiselect: ['jquery'],
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
        '../../admin/js/HandlebarsHelpers',
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
            $("#page-loading").addClass('hide');
        });

    });
}());