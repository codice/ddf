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
        'wreqr',
        'backboneassociations'
    ],
    function (Backbone, _, wreqr) {
        "use strict";
        var MetaCard = {};

        MetaCard.Geometry = Backbone.AssociatedModel.extend({

            isPoint: function () {
                return this.get('type') === 'Point';
            },

            getPoint: function () {
                return _.object(['longitude', 'latitude'], this.getAllPoints()[0]);
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
            }

        });

        MetaCard.Properties = Backbone.AssociatedModel.extend({

        });

        MetaCard.Action = Backbone.AssociatedModel.extend({

        });

        MetaCard.Metacard = Backbone.AssociatedModel.extend({
            url: '/services/catalog/',

            initialize: function () {
                this.listenTo(wreqr.vent, 'metacard:selected', _.bind(this.onAppContext, this));
            },

            onAppContext: function (direction, model) {
                if (model !== this) {
                    this.set('context', false);
                } else {
                    this.set('context', true);
                }
            },

            relations: [
                {
                    type: Backbone.One,
                    key: 'geometry',
                    relatedModel: MetaCard.Geometry
                },
                {
                    type: Backbone.One,
                    key: 'properties',
                    relatedModel: MetaCard.Properties
                },
                {
                    type: Backbone.Many,
                    key: 'actions',
                    relatedModel: MetaCard.Action
                }
            ]
        });

        MetaCard.MetacardResult = Backbone.AssociatedModel.extend({
            relations: [
                {
                    type: Backbone.One,
                    key: 'metacard',
                    relatedModel: MetaCard.Metacard
                }
            ]
        });

        MetaCard.SourceStatus = Backbone.AssociatedModel.extend({

        });

        MetaCard.SearchResult = Backbone.AssociatedModel.extend({
            relations: [
                {
                    type: Backbone.Many,
                    key: 'results',
                    relatedModel: MetaCard.MetacardResult
                },
                {
                    type: Backbone.Many,
                    key: 'status',
                    relatedModel: MetaCard.SourceStatus
                }
            ],
            url: "/service/query",
            parse: function (resp) {
                if (resp.data) {
                    return resp.data;
                }
                return resp;
            },
            cancel: function() {
                this.unsubscribe();
                if(this.has('status')){
                    var statuses = this.get('status');
                    statuses.forEach(function(status) {
                        if(status.get('state') === "ACTIVE") {
                            status.set({'canceled': true});
                        }
                    });
                }
            },
            mergeLatest: function () {
                if (this.lastResponse) {
                    var update = this.parse(this.lastResponse);
                    if (update && this.get('results') && this.get('results').length > 0) {
                        var selectedForSave = this.get('results').filter(function (result) {
                            return result.get('metacard').has('selectedForSave') &&
                                result.get('metacard').get('selectedForSave') === true;
                        });

                        _.forEach(update.results, function (result) {
                            if (_.some(selectedForSave, function (saved) {
                                return saved.get('metacard').get('properties').get('id') ===
                                    result.metacard.properties.id;
                            })) {
                                result.metacard.selectedForSave = true;
                            }
                        });
                    }

                    return this.set(update);
                }
            }
        });
        return MetaCard;

    });