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
        '/configurations/js/view/ConfigurationEdit.view.js', //TODO this import should be done better.
        '/configurations/js/model/Service.js',
        '/configurations/js/view/Service.view.js' // TODO this import should be done better.
    ], function(Marionette, _, ConfigurationView, ConfigurationModel, ServiceView){
        "use strict";

        var FeatureController = Marionette.Controller.extend({

            initialize: function(options){
                this.region = options.region;
            },

            show: function(appName){
                var self = this;
                self.appName = appName;
                var configurations = new ConfigurationModel.Response();
                configurations.fetch({
                    url: "/jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/getServices/" + appName,
                    success: function() {
                        var servicePage = new ServiceView.ServicePage({model: configurations});
                        self.region.show(servicePage);
                    }
                });
            }
        });

        return FeatureController;

    }
);