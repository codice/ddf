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
/*global define*/
define([
        'marionette',
        'underscore',
        'q',
        'js/models/module/SystemInformation',
        'js/models/module/OperatingSystem',
        'js/views/module/SystemInformation.view'
    ], function(Marionette, _, Q, SystemInformation, OperatingSystem, SystemInformationView){
        "use strict";

        var SystemInformationController = Marionette.Controller.extend({

            initialize: function(options){
                this.region = options.region;
            },

            show: function(){
                var self = this;
                var systemInformation = new SystemInformation.Model();
                var operatingSystem = new OperatingSystem.Model();

                Q.all(systemInformation.fetch(), operatingSystem.fetch()).then(
                    function() {
                        var servicePage = new SystemInformationView({systemInformation: systemInformation, operatingSystem: operatingSystem});
                        self.region.show(servicePage);
                   }
                ).fail(
                    function(error) {
                        console.log(error);
                    }
                );
            }
        });

        return SystemInformationController;

    }
);