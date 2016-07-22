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
    'wreqr',
    'backbone',
    'properties',
    './Alert',
    'backboneassociations'
], function (_, wreqr, Backbone, properties, Alert) {
    'use strict';

    var User = {};

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
        } else {
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
        defaults: {
            pointColor: '#FFA467',
            multiPointColor: '#FFA467',
            lineColor: '#5B93FF',
            multiLineColor: '#5B93FF',
            polygonColor: '#FF6776',
            multiPolygonColor: '#FF6776',
            geometryCollectionColor: '#FFFF67'
        },
        savePreferences: function () {
            this.parents[0].savePreferences();
        }
    });

    User.MapLayer = Backbone.AssociatedModel.extend({});

    User.MapLayers = Backbone.Collection.extend({
        model: User.MapLayer,
        defaults: function () {
            return _.map(_.values(mapLayerConfigForURL), function (layerConfig) {
                return layerConfig;
            });
        },
        initialize: function (models) {
            if (!models || models.length === 0) {
                this.set(this.defaults());
            }
        },
        comparator: function (model) {
            return model.get('index');
        },
        getMapLayerConfig: function (url) {
            return mapLayerConfigForURL[url];
        },
        savePreferences: function () {
            this.parents[0].savePreferences();
        }
    });

    User.Preferences = Backbone.AssociatedModel.extend({
        useAjaxSync: true,
        url: '/search/catalog/internal/user/preferences',
        defaults: function () {
            return {
                id: 'preferences',
                mapColors: new User.MapColors(),
                mapLayers: new User.MapLayers(),
                resultDisplay: 'List',
                resultPreview: ['modified'],
                resultFilter: undefined,
                resultSort: undefined,
                homeFilter: 'Owned by anyone',
                homeSort: 'Last modified',
                homeDisplay: 'Grid',
                alerts: [],
                alertPersistance: false, // don't persist across sessions by default
                alertExpiration: 86400000, // 1 day
                resultBlacklist: [],
                visualization: 'map'
            };
        },
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
            },
            {
                type: Backbone.Many,
                key: 'alerts',
                relatedModel: Alert
            }
        ],
        initialize: function(){
            if (!this.get('alertPersistance')) {
                this.get('alerts').reset();
            } else {
                var expiredAlerts = this.get('alerts').filter(function(alert){
                        var recievedAt = (new Date(alert.get('when'))).getTime();
                        return ((Date.now() - recievedAt) > this.get('alertExpiration'));
                }.bind(this));
                this.get('alerts').remove(expiredAlerts);
            }
            this.listenTo(wreqr.vent, 'alerts:add', this.addAlert);
            this.listenTo(this.get('alerts'), 'remove', this.handleRemove);
            this.listenTo(this, 'change:visualization', this.savePreferences);
        },
        handleRemove: function(){
            this.savePreferences();
        },
        addAlert: function(alertDetails){
            this.get('alerts').add(alertDetails);
            this.savePreferences();
        },
        savePreferences: function (options) {
            if (this.parents[0].isGuestUser()) {
                window.localStorage.setItem('preferences', JSON.stringify(this.toJSON()));
            } else {
                this.sync('update', this, options || {});
            }
        },
        resetBlacklist: function(){
            this.set('resultBlacklist', []);
        }
    });

    User.Model = Backbone.AssociatedModel.extend({
        defaults: function () {
            return {
                id: 'user',
                preferences: new User.Preferences(),
                isGuest: true,
                username: 'guest',
                roles: ['guest']
            };
        },
        relations: [
            {
                type: Backbone.One,
                key: 'preferences',
                relatedModel: User.Preferences
            }
        ],
        isGuestUser: function () {
            return this.get('isGuest');
        }
    });

    User.Response = Backbone.AssociatedModel.extend({
        useAjaxSync: true,
        url: '/search/catalog/internal/user',
        relations: [
            {
                type: Backbone.One,
                key: 'user',
                relatedModel: User.Model
            }
        ],
        initialize: function () {
            this.set('user', new User.Model());
            this.fetch();
        },
        getGuestPreferences: function () {
            try {
                return JSON.parse(window.localStorage.getItem('preferences')) || {};
            } catch (e) {
                return {};
            }
        },
        parse: function (body) {
            if (body.isGuest) {
                return {
                    user: _.extend({id: 'user'}, body, {
                        preferences: _.extend(
                            {id: 'preferences'},
                            this.getGuestPreferences()
                        )
                    })
                };
            } else {
                _.extend(body.preferences, { id: 'preferences'});
                return {
                    user: _.extend({
                        id: 'user'
                    }, body)
                };
            }
        },
    });

    return User;
});
