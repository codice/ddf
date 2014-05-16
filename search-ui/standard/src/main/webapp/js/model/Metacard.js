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
/*global define*/

define(function (require) {
    "use strict";
    var Backbone = require('backbone'),
        _ = require('underscore'),
        app = require('application'),
        Util = require('js/model/util'),
        properties = require('properties'),
        wreqr = require('wreqr'),
        MetaCard = app.module();

    require('backbonerelational');
    MetaCard.Geometry = Backbone.RelationalModel.extend({

        isPoint: function () {
            return this.get('type') === 'Point';
        },
        
        average: function (points, attribute) {
            var attrs = _.pluck(points, attribute);
            var sum = _.reduce(attrs, function (a, b) {
                return Math.abs(a) + Math.abs(b);
            }, 0);
            return sum / points.length;
        },
        
        getPoint: function () {
            if (this.isPolygon()) {
                var polygon = this.getPolygon(),
                    region = new Util.Region(polygon),
                    centroid = region.centroid();
                if (_.isNaN(centroid.latitude)) {
                    // seems to happen when regions is perfect rectangle...
                    if (typeof console !== 'undefined') {
                        console.warn('centroid util did not return a good centroid, defaulting to average of all points');
                    }
                    return {
                        latitude: this.average(polygon, 'latitude'),
                        longitude: this.average(polygon, 'longitude')
                    };
                } else {
                    if (typeof console !== 'undefined') {
                        console.log('centroid worked?');
                    }
                }
                return centroid;
            }
            var coordinates = this.get('coordinates');

            return this.convertPointCoordinate(coordinates);

        },
        convertPointCoordinate: function (coordinate) {
            return {
                latitude: coordinate[1],
                longitude: coordinate[0],
                altitude: coordinate[2]
            };
        },

        isPolygon: function () {
            return this.get('type') === 'Polygon';
        },
        getPolygon: function () {
            if (!this.isPolygon()) {
                if (typeof console !== 'undefined') {
                    console.log('This is not a polygon!! ', this);
                }
                return;
            }
            var coordinates = this.get('coordinates')[0];
            return _.map(coordinates, this.convertPointCoordinate);
        }

    });

    MetaCard.Properties = Backbone.RelationalModel.extend({

    });

    MetaCard.Metacard = Backbone.RelationalModel.extend({
        url: '/services/catalog/',

        initialize: function () {
            this.on('change:context', this.onChangeContext);
        },

        onChangeContext: function () {
            if (this.get('context')) {
                wreqr.vent.trigger('metacard:selected', this);
                wreqr.vent.on('metacard:selected', _.bind(this.onAppContext, this), this);
            }
        },

        onAppContext: function (model) {
            if (model !== this) {
                wreqr.vent.stopListening('metacard:selected', null, this);
                this.set('context', false);
            }
        },

        relations: [
            {
                type: Backbone.HasOne,
                key: 'geometry',
                relatedModel: MetaCard.Geometry
            },
            {
                type: Backbone.HasOne,
                key: 'properties',
                relatedModel: MetaCard.Properties
            }

        ]
    });

    MetaCard.MetacardResult = Backbone.RelationalModel.extend({
        relations: [
            {
                type: Backbone.HasOne,
                key: 'metacard',
                relatedModel: MetaCard.Metacard,
                includeInJSON: false,
                reverseRelation: {
                    key: 'metacardResult'
                }
            }
        ]
    });

    MetaCard.MetacardList = Backbone.Collection.extend({
        model: MetaCard.MetacardResult
    });

    MetaCard.SearchResult = Backbone.RelationalModel.extend({
        defaults: {
            count: properties.resultCount,
            startIndex: 1,
            format: "geojson",
            queryParamDefaults: {
                count: "&count=",
                format: "&format=",
                start: "&start="
            }
        },
        relations: [
            {
                type: Backbone.HasMany,
                key: 'results',
                relatedModel: MetaCard.MetacardResult,
                collectionType: MetaCard.MetacardList,
                includeInJSON: false,
                reverseRelation: {
                    key: 'searchResult'
                }
            }
        ],
        url: "/service/query",
        syncUrl: "/services/catalog/query",
        useAjaxSync: false,
        parse: function (resp) {
            if (resp.data) {
                return resp.data;
            }
            return resp;
        },
        getQueryParams: function () {
            var queryParams = this.get("queryParams");
            queryParams.count = this.get("count");
            queryParams.start = this.get("startIndex");
            queryParams.format = this.get("format");
            return queryParams;
        }
    });
    return MetaCard;

});