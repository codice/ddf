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

            // templates
            applicationWrapperTemplate: '/installer/templates/application.handlebars',
            applicationTemplate: '/installer/lib/application-module/templates/application.handlebars',
            applicationNodeTemplate: '/installer/lib/application-module/templates/applicationNode.handlebars',
            detailsTemplate: '/installer/lib/application-module/templates/details.handlebars',
            applicationNew: '/installer/lib/application-module/templates/applicationNew.handlebars',
            mvnItemTemplate: '/installer/lib/application-module/templates/mvnUrlItem.handlebars',
            fileProgress: '/installer/lib/application-module/templates/fileProgress.handlebars'
        }
    });
}());