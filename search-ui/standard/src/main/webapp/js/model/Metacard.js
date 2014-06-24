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

define([
        'backbone',
        'underscore',
        'js/model/util',
        'properties',
        'wreqr',
        'backbonerelational'
    ],
    function (Backbone, _, Util, properties, wreqr) {
        "use strict";
        var MetaCard = {};

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
                if (this.isPoint()) {
                    var coordinates = this.get('coordinates');
                    return this.convertPointCoordinate(coordinates);
                }
                if (this.isMultiPoint()) {
                    return this.getMultiPoint()[0];
                }

                var points = [];

                if (this.isPolygon()) {
                    points = this.getPolygon();
                } else if (this.isMultiPolygon()) {
                    points = this.getMultiPolygon()[0];
                } else if (this.isGeometryCollection()) {
                    var geo = new MetaCard.Geometry(this.getGeometryCollection()[0]);
                    points = geo.getPoint();
                } else if (this.isLineString()) {
                    points = this.getLineString();
                } else if (this.isMultiLineString()) {
                    points = this.getMultiLineString()[Math.floor(this.getMultiLineString().length / 2)];
                }

                if (this.isLineString() || this.isMultiLineString()) {
                    if (points.length % 2) {
                        points = [points[Math.floor(points.length / 2)]];
                    } else {
                        points = [points[Math.floor(points.length / 2) - 1], points[Math.floor(points.length / 2)]];
                    }
                }

                var region = new Util.Region(points),
                    centroid = region.centroid(this.isPolygon() || this.isMultiPolygon());
                if (_.isNaN(centroid.latitude)) {
                    // seems to happen when regions is perfect rectangle...
                    if (typeof console !== 'undefined') {
                        console.warn('centroid util did not return a good centroid, defaulting to average of all points');
                    }
                    return {
                        latitude: this.average(points, 'latitude'),
                        longitude: this.average(points, 'longitude')
                    };
                } else {
                    if (typeof console !== 'undefined') {
                        console.log('centroid worked?');
                    }
                }
                return centroid;
            },

            getAllPoints: function () {
                var coordinates = this.get('coordinates');

                if (this.isPoint()) {
                    return [coordinates];
                }

                if (this.isMultiPoint() || this.isLineString()) {
                    return coordinates;
                }

                if (this.isMultiLineString()) {
                    return _.flatten(coordinates, true);
                }

                if (this.isPolygon()) {
                    return coordinates[0];
                }

                if (this.isMultiPolygon()) {
                    return _.flatten(_.map(coordinates, function (instance) {
                        return instance[0];
                    }), true);
                }

                if (this.isGeometryCollection()) {
                    var geometries = this.get('geometries');
                    return _.flatten(_.map(geometries, function (geometry) {
                        var geoModel = new MetaCard.Geometry(geometry);
                        return geoModel.getAllPoints();
                    }), true);
                }
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
            },

            isLineString: function () {
                return this.get('type') === 'LineString';
            },
            getLineString: function () {
                if (!this.isLineString()) {
                    if (typeof console !== 'undefined') {
                        console.log('This is not a LineString!! ', this);
                    }
                    return;
                }
                var coordinates = this.get('coordinates');
                return _.map(coordinates, this.convertPointCoordinate);
            },

            isMultiLineString: function () {
                return this.get('type') === 'MultiLineString';
            },

            getMultiLineString: function () {
                if (!this.isMultiLineString()) {
                    if (typeof console !== 'undefined') {
                        console.log('This is not a MultiLineString!! ', this);
                    }
                    return;
                }

                var coordinates = this.get('coordinates');
                var model = this;
                return _.map(coordinates, function (instance) {
                    return _.map(instance, model.convertPointCoordinate);
                });
            },

            isMultiPoint: function () {
                return this.get('type') === 'MultiPoint';
            },

            getMultiPoint: function () {
                if (!this.isMultiPoint()) {
                    if (typeof console !== 'undefined') {
                        console.log('This is not a MultiPoint!! ', this);
                    }
                    return;
                }

                var coordinates = this.get('coordinates');
                return _.map(coordinates, this.convertPointCoordinate);
            },

            isMultiPolygon: function () {
                return this.get('type') === 'MultiPolygon';
            },

            getMultiPolygon: function () {
                if (!this.isMultiPolygon()) {
                    if (typeof console !== 'undefined') {
                        console.log('This is not a MultiPolygon!! ', this);
                    }
                    return;
                }

                var coordinates = this.get('coordinates');
                var model = this;
                return _.map(coordinates, function (instance) {
                    return _.map(instance[0], model.convertPointCoordinate);
                });
            },

            isGeometryCollection: function () {
                return this.get('type') === 'GeometryCollection';
            },

            getGeometryCollection: function () {
                if (!this.isGeometryCollection()) {
                    if (typeof console !== 'undefined') {
                        console.log('This is not a GeometryCollection!! ', this);
                    }
                    return;
                }

                var geometries = this.get('geometries');

                return _.map(geometries, function (geometry) {
                    return new MetaCard.Geometry(geometry);
                });
            },

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

        MetaCard.SourceResult = Backbone.RelationalModel.extend({

        });

        MetaCard.SourceList = Backbone.Collection.extend({
            model: MetaCard.SourceResult
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
                },
                {
                    type: Backbone.HasMany,
                    key: 'sources',
                    relatedModel: MetaCard.SourceResult,
                    collectionType: MetaCard.SourceList,
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
            },
            cancel: function() {
                this.unsubscribe();
                var sources = this.get('sources');
                sources.forEach(function(source) {
                    if(!source.get('done')) {
                        source.set({'canceled': true});
                    }
                });
            }
        });
        return MetaCard;

    });