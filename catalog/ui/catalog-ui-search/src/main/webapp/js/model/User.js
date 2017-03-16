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
    'js/Common',
    'js/model/UploadBatch',
    'component/announcement',
    'component/blacklist-item/blacklist-item',
    'backboneassociations'
], function (_, wreqr, Backbone, properties, Alert, Common, UploadBatch, announcement, BlackListItem) {
    'use strict';

    var User = {};

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
                id: Common.generateUUID()
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
                visualization: '3dmap',
                columnHide: [],
                columnOrder: ['title', 'created', 'modified', 'thumbnail'],
                uploads: [],
                fontSize: '16',
                resultCount: properties.resultCount
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
            },
            {
                type: Backbone.Many,
                key: 'uploads',
                relatedModel: UploadBatch
            },
            {
                type: Backbone.Many,
                key: 'resultBlacklist',
                relatedModel: BlackListItem
            }
        ],
        initialize: function(){
            this.handleAlertPersistance();
            this.handleResultCount();
            this.listenTo(wreqr.vent, 'alerts:add', this.addAlert);
            this.listenTo(wreqr.vent, 'uploads:add', this.addUpload);
            this.listenTo(wreqr.vent, 'preferences:save', this.savePreferences);
            this.listenTo(this.get('alerts'), 'remove', this.savePreferences);
            this.listenTo(this.get('uploads'), 'remove', this.savePreferences);
            this.listenTo(this, 'change:visualization', this.savePreferences);
            this.listenTo(this, 'change:fontSize', this.savePreferences);
        },
        handleRemove: function(){
            this.savePreferences();
        },
        addUpload: function(upload){
            this.get('uploads').add(upload);
            this.savePreferences();
        },
        addAlert: function(alertDetails){
            this.get('alerts').add(alertDetails);
            this.savePreferences();
        },
        savePreferences: function () {
            if (this.parents[0].isGuestUser()) {
                window.localStorage.setItem('preferences', JSON.stringify(this.toJSON()));
            } else {
                this.save(this.attributes, {
                    drop: true,
                    customErrorHandling: true,
                    error: function(){
                        announcement.announce({
                            title: 'Issue Authorizing Request',
                            message: 'You appear to have been logged out.  Please sign in again.',
                            type: 'error'
                        });
                    }.bind(this)
                });
            }
        },
        resetBlacklist: function(){
            this.set('resultBlacklist', []);
        },
        handleResultCount: function(){
            this.set('resultCount', Math.min(properties.resultCount, this.get('resultCount')));
        },
        handleAlertPersistance: function(){
            if (!this.get('alertPersistance')) {
                this.get('alerts').reset();
                this.get('uploads').reset();
            } else {
                var expiration = this.get('alertExpiration');
                this.removeExpiredAlerts(expiration);
                this.removeExpiredUploads(expiration);
            }
        },
        removeExpiredAlerts: function(expiration){
            var expiredAlerts = this.get('alerts').filter(function(alert){
                var recievedAt = alert.getTimeComparator();
                return ((Date.now() - recievedAt) > expiration);
            });
            this.get('alerts').remove(expiredAlerts);
        },
        removeExpiredUploads: function(expiration){
            var expiredUploads = this.get('uploads').filter(function(upload){
                var recievedAt = upload.getTimeComparator();
                return ((Date.now() - recievedAt) > expiration);
            });
            this.get('uploads').remove(expiredUploads);
        },
        parse: function(data, options){
            if (options && options.drop) {
                return {};
            }
            return data;
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
        fetched: false,
        initialize: function () {
            this.listenTo(this, 'sync', this.handleSync);
            this.set('user', new User.Model());
            this.fetch();
        },
        handleSync: function(){
            this.fetched = true;
            this.get('user').get('preferences').handleAlertPersistance();
            this.get('user').get('preferences').handleResultCount();
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
