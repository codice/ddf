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
/*global define */
define(['jquery',
        'underscore',
        'marionette'
    ], function ($, _, Marionette) {
        'use strict';
        var ApplicationController;

        ApplicationController = Marionette.Controller.extend({
            initialize: function (options) {
                _.bindAll(this);
                this.Application = options.Application;
            }

        });

        return ApplicationController;
    }
);
