/*global define*/

define(function (require) {
    "use strict";
    var Backbone = require('backbone'),
        _ = require('underscore'),
        ddf = require('ddf'),
        Util = require('js/model/util'),
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
        defaults: {
            title: "[No Title]"
        },
        initialize: function() {
            if (!this.get("title")) {
                this.set({"title": this.defaults.title});
            }
        }
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
        url: "/services/catalog/query",
        loadMoreResults: function () {
            var queryParams;
            this.set("startIndex", this.get("startIndex") + this.get("itemsPerPage"));
            queryParams = this.getQueryParams();
            return this.fetch({
                update: true,
                remove: false,
                data: queryParams,
                dataType: "jsonp",
                timeout: 300000
            });
        },
        getQueryParams: function () {
            return this.get("queryParams") + this.get("queryParamDefaults").count + this.get("count") +
                this.get("queryParamDefaults").start + this.get("startIndex") +
                this.get("queryParamDefaults").format + this.get("format");
        },
        getResultCenterPoint: function() {
            var options, avgCartographic, meanMetacard, i = 0,
                quadrantWeights = {
                    one: 1,
                    two: 1,
                    three: 1,
                    four: 1
                }, quadrantCounts = [
                    {
                        quad: 'one',
                        count: 1
                    },
                    {
                        quad: 'two',
                        count: 1
                    },
                    {
                        quad: 'three',
                        count: 1
                    },
                    {
                        quad: 'four',
                        count: 1
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

            quadrantWeights[quadrantCounts[0].quad] = Math.ceil(quadrantCounts[0].count / quadrantCounts[3].count);

            this.get("results").each(function(item) {
                if(!avgCartographic) {
                    if(item.get("metacard").get("geometry")) {
                        avgCartographic = item.get("metacard").get("geometry").getPoint();
                        i++;
                    }
                }
                else {
                    if(item.get("metacard").get("geometry")) {
                        var weightedLat = avgCartographic.latitude * i, weightedLong = avgCartographic.longitude * i,
                            newPoint = item.get("metacard").get("geometry").getPoint(), weight;
                        i++;

                        if(newPoint.longitude > 0 && newPoint.latitude > 0) {
                            weight = quadrantWeights.one;
                        }
                        else if(newPoint.longitude < 0 && newPoint.latitude > 0) {
                            weight = quadrantWeights.two;
                        }
                        else if(newPoint.longitude < 0 && newPoint.latitude < 0) {
                            weight = quadrantWeights.three;
                        }
                        else {
                            weight = quadrantWeights.four;
                        }

                        if(quadrantWeights[quadrantCounts[0].quad] > 1 && weight > 1) {
                            avgCartographic.latitude = (weightedLat + newPoint.latitude) / i;
                            avgCartographic.longitude = (weightedLong + newPoint.longitude ) / i;
                        }
                        else if(quadrantWeights[quadrantCounts[0].quad] === 1 && !avgCartographic.latitude && !avgCartographic.longitude) {
                            avgCartographic.latitude = newPoint.latitude;
                            avgCartographic.longitude = newPoint.longitude;
                        }
                    }
                }
            });
            options = {
                properties: {},
                type: "Feature",
                geometry: {
                    type: "Point",
                    coordinates: [
                        avgCartographic ? avgCartographic.longitude : 0,
                        avgCartographic ? avgCartographic.latitude : 0,
                        5000000
                    ]
                }
            };
            meanMetacard = new MetaCard.Metacard(options);
            return meanMetacard;
        }
    });
    return MetaCard;

});