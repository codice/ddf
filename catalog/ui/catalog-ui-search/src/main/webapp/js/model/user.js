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
/*global define, window, parseFloat*/

define([
    'underscore',
    'backbone',
    'properties',
    'backboneassociations'
], function (_, Backbone, properties) {
    'use strict';

    var User = {};

    var defaultColors = {
        pointColor: '#FFA467',
        multiPointColor: '#FFA467',
        lineColor: '#5B93FF',
        multiLineColor: '#5B93FF',
        polygonColor: '#FF6776',
        multiPolygonColor: '#FF6776',
        geometryCollectionColor: '#FFFF67'
    };

    var mapLayerConfigForURL = {};

    _.each(properties.imageryProviders, function (providerConfig, index) {
        /*
         - prefer configured 'index' value
         - fallback to implicit order in admin ui.
         - duplicate indexes sorted arbitrarily.
         - allow semantic values like -1000 ~ "lowest", 1000 ~ "highest"
         */
        var configIndex;
        if (providerConfig.index && !isNaN(Number(providerConfig.index))) {
            configIndex = Number(providerConfig.index);
        }
        else {
            configIndex = index;
        }

        var label = providerConfig.label ? providerConfig.label : 'Layer ' + configIndex;
        var alpha = providerConfig.alpha ? parseFloat(providerConfig.alpha) : 0.5;
        var show = providerConfig.show ? providerConfig.show : true;

        var defaultConfig = {
            label: label,
            show: show,
            index: configIndex,
            alpha: alpha
        };
        var layerConfig = _.extend(defaultConfig, providerConfig);

        // index provider config by unique url; supports resetting defaults.
        mapLayerConfigForURL[layerConfig.url] = layerConfig;
    });

    _.chain(_.values(mapLayerConfigForURL))
        .sortBy(function (layerConfig) {
            return layerConfig.index;
        })
        .each(function (layerConfig, index) {
            // smooth config errors and semantic values.
            layerConfig.index = index;
        }).value();

    User.MapColors = Backbone.AssociatedModel.extend({
        getDefaults: function () {
            return defaultColors;
        },
        savePreferences: function () {
            if (this.parents[0].parents[0].isGuestUser()) {
                window.localStorage.setItem('org.codice.ddf.search.preferences.mapColors', JSON.stringify(this.toJSON()));
            } else {
                this.parents[0].savePreferences();
            }
        }
    });

    User.MapLayer = Backbone.AssociatedModel.extend({});

    User.MapLayers = Backbone.Collection.extend({
        model: User.MapLayer,
        comparator: function (model) {
            return model.get('index');
        },
        getMapLayerConfig: function (url) {
            return mapLayerConfigForURL[url];
        },
        savePreferences: function () {
            if (this.parents[0].parents[0].isGuestUser()) {
                var mapLayersJson = JSON.stringify(this.toJSON());
                window.localStorage.setItem('org.codice.ddf.search.preferences.mapLayers', mapLayersJson);
            } else {
                // Backbone.Collection does not have a builtin save().
                this.parents[0].savePreferences();
            }
        }
    });

    User.Preferences = Backbone.AssociatedModel.extend({
        url: '/service/user',
        relations: [
            {
                type: Backbone.One,
                key: 'mapColors',
                relatedModel: User.MapColors
            },
            {
                type: Backbone.Many,
                key: 'mapLayers',
                relatedModel: User.MapLayer,
                collectionType: User.MapLayers
            }
        ],
        savePreferences: function () {
            this.save();
        }
    });

    User.Model = Backbone.AssociatedModel.extend({
        relations: [
            {
                type: Backbone.One,
                key: 'preferences',
                relatedModel: User.Preferences
            }
        ],
        isGuestUser: function () {
            return this.get('isGuest') === 'true' || this.get('isGuest') === true;
        }
    });

    User.Response = Backbone.AssociatedModel.extend({
        relations: [
            {
                type: Backbone.One,
                key: 'user',
                relatedModel: User.Model
            }
        ],
        url: '/service/user',
        initialize: function () {
            var user = new User.Model();
            this.set('user', user);

            var preferences = new User.Preferences();
            user.set({
                preferences: preferences,
                isGuest: true,
                username: 'guest'
            });

            preferences.set('mapColors', this.getFallbackMapColors());
            preferences.set('mapLayers', this.getFallbackMapLayers());
        },
        parse: function (resp) {
            var parsedData = resp.data ? resp.data : resp;

            if (undefined === parsedData.user.preferences) {
                var preferences = {};
                preferences.mapColors = this.getFallbackMapColors();
                preferences.mapLayers = this.getFallbackMapLayers();
                parsedData.user.preferences = preferences;
            }
            return parsedData;
        },
        getFallbackMapColors: function () {
            var jsonString = window.localStorage.getItem('org.codice.ddf.search.preferences.mapColors');
            var colorPreferences;
            if (jsonString && jsonString !== '') {
                colorPreferences = JSON.parse(jsonString);
            }
            else {
                colorPreferences = defaultColors;
            }
            return colorPreferences;
        },
        getFallbackMapLayers: function () {
            var layerPreferences = [];
            var jsonString = window.localStorage.getItem('org.codice.ddf.search.preferences.mapLayers');
            if (jsonString && jsonString !== '') {
                layerPreferences = JSON.parse(jsonString);
            }
            else {
                _.each(_.values(mapLayerConfigForURL), function (layerConfig) {
                    layerPreferences.push(layerConfig);
                });
            }
            return layerPreferences;
        }
    });

    return User;
});
