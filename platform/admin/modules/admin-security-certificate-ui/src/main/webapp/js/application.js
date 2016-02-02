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
/*global define,require*/

// #Main Application
define([
    'marionette'
], function (Marionette) {
    'use strict';

    var Application = {};

    Application.App = new Marionette.Application();

    //add regions
    Application.App.addRegions({
        mainRegion: 'main'
    });

    Application.App.addInitializer(function () {
        require(['js/module']);
    });

    return Application;
});
