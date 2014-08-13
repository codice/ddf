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
        '/applications/js/view/features/features.view.js',
        '/applications/js/model/features/feature.js'
    ], function(Marionette, _, FeaturesView, FeatureModel){
        "use strict";

        var FeatureController = Marionette.Controller.extend({

            initialize: function(options){
                this.region = options.region;
            },

            getFeatures: function(appName){
                var self = this;
                self.appName = appName;
                var features = new FeatureModel.Collection({
                    type: 'app',
                    appName: appName
                });
                features.fetch({
                    success: function(collection) {
                        var featureView = new FeaturesView({
                            collection: collection
                        });
                        self.region.show(featureView);
                        self.listenTo(featureView,"itemview:selected", self.onFeatureAction);
                    }
                });
            },

            show: function(appName){
                var self = this;
                self.appName = appName;
                var features = new FeatureModel.Collection({
                    type: 'app',
                    appName: appName
                });
                features.fetch({
                    success: function(collection) {
                        var featureView = new FeaturesView({
                            collection: collection
                        });
                        self.region.show(featureView);
                        self.listenTo(featureView,"itemview:selected", self.onFeatureAction);
                    }
                });
            },

            showAll: function(){
                var view = this;
                var features = new FeatureModel.Collection({
                    type: 'all'
                });
                features.fetch({
                    success: function(collection) {
                        var featureView = new FeaturesView({
                            collection: collection
                        });
                        view.region.show(featureView);
                    }
                });
            },

            onFeatureAction: function (view, model){
                var self = this;
                var status = model.get("status");
                var featureModel = new FeatureModel.Model({
                    name: model.get("name")
                });
                //TODO: add loading div...
                if(status === "Uninstalled") {
                    var install = featureModel.install();
                    if(install){
                        install.success(function() {
                            self.show(self.appName);
                        }).fail(function() {
                            if(console) {
                                console.log("install failed for feature: " + featureModel.name + " app: " + self.appName);
                            }
                        });
                    }
                }else{
                    var uninstall = featureModel.uninstall();
                    if(uninstall){
                        uninstall.success(function() {
                            self.show(self.appName);
                        }).fail(function() {
                            if(console) {
                                console.log("uninstall failed for feature: " + featureModel.name + " app: " + self.appName);
                            }
                        });
                    }
                }
            }


        });

        return FeatureController;

    }
);