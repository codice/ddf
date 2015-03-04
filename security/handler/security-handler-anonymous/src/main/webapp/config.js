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
            q: 'lib/q/q',

            // backbone
            backbone: 'lib/components-backbone/backbone-min',
            underscore: 'lib/lodash/dist/lodash.underscore.min',
            marionette: 'lib/marionette/lib/backbone.marionette.min',

            // jquery
            jquery: 'lib/jquery/jquery.min',
            jqueryuiCore: 'lib/jquery-ui/ui/minified/jquery.ui.core.min',

            // purl
            purl: 'lib/purl/purl',

            // handlebars
            handlebars: 'lib/handlebars/handlebars.min',
            icanhaz: 'lib/icanhandlebarz/ICanHandlebarz',

            // require plugins
            text: 'lib/requirejs-plugins/lib/text',

            // default login ui
            app: 'js/application'
        },


        shim: {

            backbone: {
                deps: ['underscore', 'jquery'],
                exports: 'Backbone'
            },
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
            bootstrap: {
                deps: ['jquery']
            }

        },

        waitSeconds: 200
    });

}());