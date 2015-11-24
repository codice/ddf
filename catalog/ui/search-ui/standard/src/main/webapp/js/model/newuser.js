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

define([
    'backbone',
    'properties',
    'cesium',
    'backboneassociations'
], function (Backbone, properties, Cesium) {
    'use strict';

    var User = {};

    User.ColorPreferences = Backbone.AssociatedModel.extend({
        defaults: {
            pointColor: '#FFA467',
            multiPointColor: '#FFA467',
            lineColor: '#5B93FF',
            multiLineColor: '#5B93FF',
            polygonColor: '#FF6776',
            multiPolygonColor: '#FF6776',
            geometryCollectionColor: '#FFFF67'
        },
        //this URL is not used for fetching, but for saving the preferences back to the user service
        url: '/service/user',
        initialize: function () {
            if (this.parents.length === 0 || this.parents[0].isGuestUser()) {
                var jsonString = window.localStorage.getItem('org.codice.ddf.search.color.preferences');
                if (jsonString && jsonString !== '') {
                    this.set(JSON.parse(jsonString));
                }
            }
        },
        savePreferences: function () {
            if (this.parents[0].isGuestUser()) {
                window.localStorage.setItem('org.codice.ddf.search.color.preferences', JSON.stringify(this.toJSON()));
            } else {
                //this call will use the above URL through the cometd backbone library
                this.save();
            }
        }
    });

    User.LayerPref = Backbone.AssociatedModel.extend({});

    User.LayerPrefsList = Backbone.Collection.extend({
        model: User.LayerPref,
        comparator: false, // disable sorting

        //this URL is not used for fetching, but for saving the preferences back to the user service
        url: '/service/user',

        /*
         * moving backing array element and calling reset() on collection makes collection view
         * re-render all child views. calling add(model), remove(model) just
         * causes the model's childview to re-render.
         */
        moveUp: function (model) {
            var index = this.indexOf(model);
            if (index < this.models.length) {
                this.modelArray.splice(index + 1, 0, this.modelArray.splice(index, 1)[0]);
                this.reset(this.modelArray);
            }
        },
        moveDown: function (model) {
            var index = this.indexOf(model);
            if (index > 0) {
                this.modelArray.splice(index - 1, 0, this.modelArray.splice(index, 1)[0]);
                this.reset(this.modelArray);
            }
        },
        initialize: function (models) {
            this.modelArray = models;
        }
    });

    User.Model = Backbone.AssociatedModel.extend({
        relations: [
            {
                type: Backbone.One,
                key: 'colorPrefs',
                relatedModel: User.ColorPreferences
            },
            {
                type: Backbone.Many,
                key: 'layerPrefs',
                relatedModel: User.LayerPref,
                collectionType: User.LayerPrefsList
            }
        ],
        initialize: function () {
            if (!this.get('colorPrefs')) {
                this.set({colorPrefs: new User.ColorPreferences()});
            }
            if (!this.get('layerPrefs')) {
                var dummyData = [
                    new User.LayerPref({
                        label: 'Grid',
                        provider: new Cesium.GridImageryProvider(),
                        alpha: 1.0,
                        show: true
                    }),
                    new User.LayerPref({
                        label: 'Tile Coordinates',
                        provider: new Cesium.TileCoordinatesImageryProvider(),
                        alpha: 0.5,
                        show: true
                    }),
                    new User.LayerPref({
                        label: 'OpenStreetMaps',
                        provider: new Cesium.OpenStreetMapImageryProvider(),
                        alpha: 0.5,
                        show: false
                    })
                ];
                var layerPrefs = new User.LayerPrefsList(dummyData);
                this.set({layerPrefs: layerPrefs});
            }
        },
        isGuestUser: function () {
            //return this.get('isAnonymous') === 'true' || this.get('isAnonymous') === true;
            return true;
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
        defaults: function () {
            return {
                user: new User.Model()
            };
        },
        parse: function (resp) {
            if (resp.data) {
                return resp.data;
            }
            return resp;
        }
    });

    return User;
});
