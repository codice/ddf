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

    require(['/error/config.js'], function () {
        require([
            'backbone',
            'marionette',
            'icanhaz',
            'js/application',
            'js/HandlebarsHelpers'

        ], function (Backbone, Marionette, ich, Application) {

            var app = Application.App;

            Marionette.Renderer.render = function (template, data) {
                if(!template){return '';}
                return ich[template](data);
            };

            if(window){
                // make ddf object available on window.  Makes debugging in chrome console much easier
                window.app = app;
            }

            // Actually start up the application.
            app.start();
        });
    });
}());