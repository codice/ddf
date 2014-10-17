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
        'js/views/configuration/ConfigurationEdit.view', //TODO this import should be done better.
        'js/models/Service',
        'js/wreqr.js',
        'js/views/configuration/Service.view' // TODO this import should be done better.
    ], function(Marionette, _, ConfigurationView, ConfigurationModel, wreqr, ServiceView){
        "use strict";

        var FeatureController = Marionette.Controller.extend({

            initialize: function(options){
                _.bindAll(this);
                this.region = options.region;
                this.listenTo(wreqr.vent, 'refreshConfigurations', this.show);
            },

            show: function(appName){
                var self = this;
                if (!self.appName) {
                    self.appName = appName;
                }
                if (self.appName) {
                    var configurations = new ConfigurationModel.Response({url: "/jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/getServices/" + self.appName});
                    configurations.fetch({
                        success: function () {
                            var servicePage = new ServiceView.ServicePage({model: configurations, url: "/jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/getServices/" + self.appName});
                            self.region.show(servicePage);
                        }
                    });
                }
            }
        });

        return FeatureController;

    }
);