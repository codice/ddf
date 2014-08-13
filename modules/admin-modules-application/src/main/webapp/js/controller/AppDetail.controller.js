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
    '/applications/js/wreqr.js',
    '/applications/js/view/application-detail/ApplicationDetail.layout.js',
    '/applications/js/model/AppConfigPlugin.js',
    '/applications/js/view/application-detail/PluginTabContent.view.js',
    '/applications/js/view/application-detail/PluginTab.view.js',
    'q'
    ],function (Marionette, wreqr, ApplicationDetailLayout, AppConfigPlugin,PluginTabContentView,PluginTabView,  Q) {

    var AppDetailController = Marionette.Controller.extend({

        initialize: function(options){
            this.regions = options.regions;
            this.listenTo(wreqr.vent,'application:selected' , this.showDetailsPage);
        },
        showDetailsPage: function(applicationModel) {
            var layoutView = new ApplicationDetailLayout({model: applicationModel});
            this.regions.applications.show(layoutView);

            this.fetchAppConfigPlugins(applicationModel.get('name')).then(function(appConfigPlugins){
                layoutView.tabs.show(new PluginTabView({collection: appConfigPlugins, model: applicationModel}));
                layoutView.tabContent.show(new PluginTabContentView({collection: appConfigPlugins, model: applicationModel}));
                layoutView.selectFirstTab();
            }).fail(function(error){
                console.log(error.stack);
                throw error;
            });
        },
        fetchAppConfigPlugins: function(appName){
            var collection = new AppConfigPlugin.Collection();
            var defer = Q.defer();
            collection.fetchByAppName(appName, {
                success: function(){
                    defer.resolve(collection);
                },
                failure: function(){
                    defer.reject(new Error("Error fetching app config plugins for {0}".format(appName)));
                }
            });
            return defer.promise;
        }
    });

    return AppDetailController;



});