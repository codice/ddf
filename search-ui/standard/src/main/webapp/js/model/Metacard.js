/*global define*/

define(function (require) {
    "use strict";
    var Backbone = require('backbone'),
        _ = require('underscore'),
        ddf = require('ddf'),
        Util = require('js/model/util'),
        Cesium = require('cesium'),
        $ = require('jquery'),
        MetaCard = ddf.module();

    require('backbonerelational');
    MetaCard.Geometry = Backbone.RelationalModel.extend({

        isPoint: function () {
            return this.get('type') === 'Point';
        },

        average : function(points, attribute){
            var attrs = _.pluck(points,attribute);
            var sum = _.reduce(attrs,function(a,b){return a+b;},0);
            return sum / points.length;
        },
        getPoint: function () {
            if (this.isPolygon()) {
                var polygon = this.getPolygon(),
                    region = new Util.Region(polygon),
                    centroid = region.centroid();
                if (_.isNaN(centroid.latitude)) {
                    // seems to happen when regions is perfect rectangle...
                    console.warn('centroid util did not return a good centroid, defaulting to average of all points');

                    return { latitude: this.average(polygon,'latitude'),
                            longitude: this.average(polygon,'longitude')};
                }else{
                    console.log('centroid worked?');
                }
                return centroid;
            }
            var coordinates = this.get('coordinates');

            return this.convertPointCoordinate(coordinates);

        },
        convertPointCoordinate: function (coordinate) {
            return {latitude: coordinate[1], longitude: coordinate[0], altitude: coordinate[2]};
        },

        isPolygon: function () {
            return this.get('type') === 'Polygon';
        },
        getPolygon: function () {
            if (!this.isPolygon()) {
                console.log('This is not a polygon!! ', this);
                return;
            }
            var coordinates = this.get('coordinates')[0];
            return _.map(coordinates, this.convertPointCoordinate);
        }

    });

    MetaCard.Properties = Backbone.RelationalModel.extend({

    });

    MetaCard.Metacard = Backbone.RelationalModel.extend({
        initialize: function () {
            this.listenTo(this, 'change:context', this.onChangeContext);
        },

        onChangeContext: function () {
            var eventBus = ddf.app,
                name = 'model:context';

            if (this.get('context')) {
                eventBus.trigger(name, this);
                this.listenTo(eventBus, name, this.onAppContext);
            }
        },

        onAppContext: function (model) {
            var eventBus = ddf.app,
                name = 'model:context';
            if (model !== this) {
                this.stopListening(eventBus, name);
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
            count: 100,
            itemsPerPage: 100,
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
                reverseRelation: {
                    key: 'searchResult'
                }
            }
        ],
        url: "/services/async/search",
        parse: function(resp) {
            return $.parseJSON(resp.data);
        },
        loadMoreResults: function () {
            var model = this;
            var queryParams;
            this.set("startIndex", this.get("startIndex") + this.get("itemsPerPage"));
            queryParams = this.getQueryParams();
            this.cometdUnbind();
            return this.fetch({
                update: true,
                remove: false,
                data: queryParams,
                dataType: "jsonp",
                timeout: 300000
            }).complete(function () {
                model.cometdBind();
            });
        },
        getQueryParams: function () {
            var queryParams = this.get("queryParams");
            queryParams.count = this.get("count");
            queryParams.start = this.get("startIndex");
            queryParams.format = this.get("format");
            return queryParams;
        },
        getResultCenterPoint: function() {
            var regionPoints = [],
                resultQuad,
                quadrantCounts = [
                    {
                        quad: 'one',
                        count: 0
                    },
                    {
                        quad: 'two',
                        count: 0
                    },
                    {
                        quad: 'three',
                        count: 0
                    },
                    {
                        quad: 'four',
                        count: 0
                    }
                ];

            this.get("results").each(function(item) {
                if(item.get("metacard").get("geometry")) {
                    var point = item.get("metacard").get("geometry").getPoint();
                    if(point.longitude > 0 && point.latitude > 0) {
                        quadrantCounts[0].count++;
                    }
                    else if(point.longitude < 0 && point.latitude > 0) {
                        quadrantCounts[1].count++;
                    }
                    else if(point.longitude < 0 && point.latitude < 0) {
                        quadrantCounts[2].count++;
                    }
                    else {
                        quadrantCounts[3].count++;
                    }
                }
            });

            quadrantCounts = _.sortBy(quadrantCounts, 'count');

            quadrantCounts.reverse();
            resultQuad = quadrantCounts[0].quad;

            this.get("results").each(function(item) {
                if(item.get("metacard").get("geometry")) {
                    var newPoint = item.get("metacard").get("geometry").getPoint(), isInRegion = false;

                    if(newPoint.longitude >= 0 && newPoint.latitude >= 0 && resultQuad === "one") {
                        isInRegion = true;
                    }
                    else if(newPoint.longitude <= 0 && newPoint.latitude >= 0 && resultQuad === "two") {
                        isInRegion = true;
                    }
                    else if(newPoint.longitude <= 0 && newPoint.latitude <= 0 && resultQuad === "three") {
                        isInRegion = true;
                    }
                    else if(newPoint.longitude >= 0 && newPoint.latitude <= 0 && resultQuad === "four") {
                        isInRegion = true;
                    }

                    if(isInRegion) {
                        regionPoints.push(newPoint);
                    }
                }
            });

            if(regionPoints.length === 0) {
                return null;
            }

            var cartPoints = _.map(regionPoints, function (point) {
                return Cesium.Cartographic.fromDegrees(point.longitude, point.latitude, point.altitude);
            });

            var extent = Cesium.Extent.fromCartographicArray(cartPoints);
            return extent;
        }
    });
    return MetaCard;

});