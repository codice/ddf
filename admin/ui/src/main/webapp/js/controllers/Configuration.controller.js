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
        'js/views/configuration/ConfigurationEdit.view',
        'js/models/Service',
        'js/wreqr.js',
        'js/views/configuration/Service.view'
    ], function(Marionette, _, ConfigurationView, ConfigurationModel, wreqr, ServiceView){
        "use strict";

        var ConfigurationController = Marionette.Controller.extend({

            initialize: function(options){
                var self = this;
                _.bindAll(this);

                self.region = options.region;
                self.listenTo(wreqr.vent, 'refreshConfigurations', this.show);
                self.url = options.url;
            },

            show: function(){
                var self = this;
                var configurations;

                if (!self.servicePage) {
                    configurations = new ConfigurationModel.Response({url: self.url});
                    configurations.fetch({
                        success: function () {
                                self.servicePage = new ServiceView.ServicePage({model: configurations, url: self.url});
                                self.region.show(self.servicePage);
                        }
                    });
                } else {
                    configurations = new ConfigurationModel.Response({url: self.url});
                    configurations.fetch({
                        success: function (freshModel) {
                            var collection = self.servicePage.model.get('value');
                            // sort by name
                            collection.comparator = function(model) {
                                return model.get('name');
                            };
                            var cfgs = freshModel.get('value');
                            var model = collection.parents[0];
                            collection.reset(cfgs.models);
                            model.trigger('services:refresh', self.servicePage.refreshButton);
                        }
                    });
                }
            }
        });

        return ConfigurationController;

    }
);