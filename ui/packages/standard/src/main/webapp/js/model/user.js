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
/*global define, window*/
/*jslint bitwise: true */

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

    function generateId() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            var r = Math.random() * 16 | 0,
                v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }

    User.updateMapLayers = function(layerPrefs, layersToRemove, index) {
        layerPrefs.each(function (layer) {
            var found = false;
            if (layer) {
                for (var i = 0; i < properties.imageryProviders.length; i++) {
                    var layerObj = _.omit(layer.toJSON(), ['id', 'show', 'label', 'alpha']);
                    var propProvider = _.omit(properties.imageryProviders[i], 'alpha');
                    if (_.isEqual(propProvider, layerObj)) {
                        found = true;
                    }
                }
            }
            if (!found) {
                layersToRemove[index++] = layer;
            }
        });
        layerPrefs.remove(layersToRemove);
        for (var i = 0; i < properties.imageryProviders.length; i++) {
            var found = false;
            for (var j = 0; j < layerPrefs.models.length; j++) {
                var layerObj = _.omit(layerPrefs.at(j).toJSON(), ['id', 'show', 'label', 'alpha']);
                var propProvider = _.omit(properties.imageryProviders[i], 'alpha');
                if (_.isEqual(propProvider, layerObj)) {
                    found = true;
                }
            }
            if (!found) {
                layerPrefs.add(new User.MapLayer(properties.imageryProviders[i], {parse: true}));
            }
        }
    };

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

    User.MapLayer = Backbone.AssociatedModel.extend({
        defaults: function() {
            return {
                alpha: 0.5,
                show: true,
                id: generateId()
            };
        },
        parse: function(resp) {
            var layer = _.clone(resp);
            layer.label = 'Type: ' + layer.type;
            if (layer.layer) {
                layer.label += ' Layer: ' + layer.layer;
            }
            if (layer.layers) {
                layer.label += ' Layers: ' + layer.layers.join(', ');
            }
            return layer;
        }
    });

    User.MapLayers = Backbone.Collection.extend({
        model: User.MapLayer,
        comparator: function (model) {
            return 1 - model.get('alpha');
        },
        getMapLayerConfig: function (url) {
            return this.findWhere({url: url});
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
                username: 'guest',
                userid: 'guest'
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
                _.each(_.values(properties.imageryProviders), function (layerConfig) {
                    layerPreferences.push(layerConfig);
                });
            }
            return layerPreferences;
        }
    });

    return User;
});
