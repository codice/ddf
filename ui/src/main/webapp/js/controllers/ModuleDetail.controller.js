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
    'js/wreqr.js',
    'js/views/module/ModuleDetail.layout',
    'js/models/module/ModulePlugin',
    'js/views/application/application-detail/PluginTabContent.view',
    'js/views/application/application-detail/PluginTab.view',
    'q'
],function (Marionette, wreqr, ModuleDetailLayout, ModulePlugin, PluginTabContentView, PluginTabView, Q) {

    var ModuleDetailController = Marionette.Controller.extend({

        initialize: function(options){
            this.regions = options.regions;
        },
        show: function() {
            var layoutView = new ModuleDetailLayout();
            this.regions.applications.show(layoutView);

            this.fetchModulePlugins(this.name).then(function(appConfigPlugins){
                layoutView.tabs.show(new PluginTabView({collection: appConfigPlugins}));
                layoutView.tabContent.show(new PluginTabContentView({collection: appConfigPlugins}));
                layoutView.selectFirstTab();
            }).fail(function(error){
                console.log(error.stack);
                throw error;
            });
        },
        fetchModulePlugins: function(moduleName){
            var collection = new ModulePlugin.Collection();
            var defer = Q.defer();
            collection.fetchByModuleName(moduleName, {
                success: function(){
                    defer.resolve(collection);
                },
                failure: function(){
                    defer.reject(new Error("Error fetching app config plugins for {0}".format(this.name)));
                }
            });
            return defer.promise;
        }
    });

    return ModuleDetailController;



});