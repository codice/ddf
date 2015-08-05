/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/* jshint unused: false */
/*global define*/
define([
    'js/application',
    'require'
],function(Application, require) {

    Application.App.module('Configurations', function(ServiceModule, App, Backbone, Marionette, $, _) {

        require([
            'js/controllers/App.controller',
            'js/controllers/ModuleDetail.controller'
        ], function(AppController, ModuleDetailController) {

            ServiceModule.addInitializer(function(){

                ServiceModule.controllers = {};
                ServiceModule.controllers.appDetailController = new ModuleDetailController({
                    regions: {
                        applications: App.configurations
                    }
                });

                // display main app home.
                ServiceModule.controllers.appDetailController.show();
            });
        });


    });
});