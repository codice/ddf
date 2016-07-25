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
    'wreqr',
    'backbone',
    'properties',
    './Alert',
    'backboneassociations'
], function (_, wreqr, Backbone, properties, Alert) {
    'use strict';

    var User = {};

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

    User.MapLayer = Backbone.AssociatedModel.extend({
        defaults: function() {
            return {
                alpha: 0.5,
                show: true,
                id: generateId()
            };
        },
        parse: function(resp) {
            resp.label = 'Type: ' + resp.type;
            if (resp.layer) {
                resp.label += ' Layer: ' + resp.layer;
            }
            if (resp.layers) {
                resp.label += ' Layers: ' + resp.layers.join(', ');
            }
            return resp;
        }
    });

    User.MapLayers = Backbone.Collection.extend({
        model: User.MapLayer,
        defaults: function () {
            return _.map(_.values(properties.imageryProviders), function (layerConfig) {
                return new User.MapLayer(layerConfig, {parse: true});
            });
        },
        initialize: function (models) {
            if (!models || models.length === 0) {
                this.set(this.defaults());
            }
        },
        comparator: function (model) {
            return 1 - model.get('alpha');
        },
        getMapLayerConfig: function (url) {
            return this.findWhere({url: url});
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
        }
    });

    return User;
});
