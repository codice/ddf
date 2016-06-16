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

            bootstrap: '../../../admin/lib/components-bootstrap/js/bootstrap.min',
            q: '../../../admin/lib/q/q',

            // backbone
            backbone: '../../../admin/lib/components-backbone/backbone-min',
            backboneassociation: '../../../admin/lib/backbone-associations/backbone-associations-min',
            underscore: '../../../admin/lib/lodash/dist/lodash.underscore.min',
            marionette: '../../../admin/lib/marionette/lib/backbone.marionette.min',
            modelbinder: '../../../admin/lib/backbone.modelbinder/Backbone.ModelBinder.min',
            poller: '../../../admin/lib/backbone-poller/backbone.poller',
            iframeresizer: '../../../admin/lib/iframe-resizer/js/iframeResizer.min',

            // jquery
            jquery: '../../../admin/lib/jquery/jquery.min',
            jqueryui: '../../../admin/lib/jquery-ui/ui/minified/jquery-ui.min',

            // handlebars
            handlebars: '../../../admin/lib/handlebars/handlebars.min',
            icanhaz: '../../../admin/lib/icanhandlebarz/ICanHandlebarz',

            // require plugins
            text: '../../../admin/lib/requirejs-plugins/lib/text'
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
        '../../../admin/js/HandlebarsHelpers',
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