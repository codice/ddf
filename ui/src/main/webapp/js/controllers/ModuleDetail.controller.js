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
    'backbone',
    'marionette',
    'js/views/module/ModuleDetail.layout',
    'js/models/module/ModulePlugin',
    'js/views/application/application-detail/PluginTabContent.view',
    'js/views/application/application-detail/PluginTab.view'
],function (Backbone, Marionette, ModuleDetailLayout, ModulePlugin, PluginTabContentView, PluginTabView) {

    var ModuleDetailController = Marionette.Controller.extend({

        initialize: function(options){
            this.regions = options.regions;
        },
        show: function() {
            var layoutView = new ModuleDetailLayout();
            this.regions.applications.show(layoutView);

            var staticModulePlugins = [
                 new Backbone.Model({
                    'id': 'systemInformationModuleTabID',
                    'displayName': 'System Information',
                    'javascriptLocation': 'js/views/module/plugins/systeminformation/Plugin.view.js'
                }),
                new Backbone.Model({
                    'id': 'featureModuleTabID',
                    'displayName': 'Features',
                    'javascriptLocation': 'js/views/module/plugins/feature/Plugin.view.js'
                }),
                new Backbone.Model({
                    'id': 'configurationModuleTabID',
                    'displayName': 'Configuration',
                    'javascriptLocation': 'js/views/module/plugins/configuration/Plugin.view.js'
                 })
            ];

            var collection = new ModulePlugin.Collection();
            collection.comparator = function(model) {
                return model.get('displayName');
            };
            collection.add(staticModulePlugins);
            collection.sort();

            layoutView.tabs.show(new PluginTabView({collection: collection}));
            layoutView.tabContent.show(new PluginTabContentView({collection: collection}));
            layoutView.selectFirstTab();
        }
    });

    return ModuleDetailController;



});