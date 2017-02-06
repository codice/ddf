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
/*global define, setInterval, clearInterval*/

define([
        'backbone',
        'underscore',
        'properties',
        'moment',
        'js/cql',
        'wellknown',
        'js/model/Metacard',
        'component/singletons/sources-instance',
        'usngs',
        'wreqr',
        'js/Common',
        'js/CacheSourceSelector',
        'component/announcement',
        'js/CQLUtils',
        'backboneassociations'
    ],
    function (Backbone, _, properties, moment, cql, wellknown, Metacard, Sources, usngs, wreqr, Common, CacheSourceSelector, announcement,
            CQLUtils) {
        "use strict";
        var Query = {};

        var converter = new usngs.Converter();

        function limitToDeleted(cqlString){
            return CQLUtils.transformFilterToCQL({
                type: 'AND',
                filters: [
                    CQLUtils.transformCQLToFilter(cqlString), 
                    {
                        property: '"metacard-tags"',
                        type: "ILIKE",
                        value: 'deleted'
                    }
                ]
            });
        }

        function limitToHistoric(cqlString){
            return CQLUtils.transformFilterToCQL({
                type: 'AND',
                filters: [
                    CQLUtils.transformCQLToFilter(cqlString),
                    {
                        property: '"metacard-tags"',
                        type: "ILIKE",
                        value: 'revision'
                    }
                ]
            });
        }

        Query.Model = Backbone.AssociatedModel.extend({
            relations: [
                {
                    type: Backbone.One,
                    key: 'result',
                    relatedModel: Metacard.SearchResult,
                    isTransient: true
                }
            ],
            //in the search we are checking for whether or not the model
            //only contains 5 items to know if we can search or not
            //as soon as the model contains more than 5 items, we assume
            //that we have enough values to search
            defaults: function() {
                return {
                    cql: "anyText ILIKE ''",
                    title: 'Search Name',
                    offsetTimeUnits: 'hours',
                    scheduleUnits: 'minutes',
                    timeType: 'modified',
                    radiusUnits: 'meters',
                    radius: 0,
                    count: properties.resultCount,
                    start: 1,
                    format: "geojson",
                    locationType: 'latlon',
                    lat: undefined,
                    lon: undefined,
                    federation: 'enterprise',
                    sortField: 'modified',
                    sortOrder: 'desc',
                    dtstart: undefined,
                    dtend: undefined,
                    result: undefined
                };
            },

            drawing: false,

            initialize: function () {
                _.bindAll(this);
                this.set('id', this.getId());
                this.listenTo(this, 'change:north change:south change:east change:west', this.setBBox);
                this.listenTo(this, 'change:scheduled change:scheduleValue change:scheduleUnits', this.startScheduledSearch);
                this.listenTo(this, 'change:bbox', this.setBboxLatLon);
                this.listenTo(this, 'change:lat change:lon', this.setRadiusLatLon);
                this.listenTo(this, 'change:usngbb', this.setBboxUsng);
                this.listenTo(this, 'change:usng', this.setRadiusUsng);
                this.listenTo(this, 'EndExtent', this.notDrawing);
                this.listenTo(this, 'BeginExtent', this.drawingOn);

                if (this.get('scheduled')) {
                    this.startSearch();
                }

                this.startScheduledSearch();
            },

            notDrawing: function () {
                this.drawing = false;
            },

            drawingOn: function () {
                this.drawing = true;
            },

            repositionLatLon: function () {
                if (this.get('usngbb')) {
                    var result = converter.USNGtoLL(this.get('usngbb'));
                    var newResult = {};
                    newResult.mapNorth = result.north;
                    newResult.mapSouth = result.south;
                    newResult.mapEast = result.east;
                    newResult.mapWest = result.west;

                    this.set(newResult);
                }
            },

            setLatLon: function () {
                var result = {};
                result.north = this.get('mapNorth');
                result.south = this.get('mapSouth');
                result.west = this.get('mapWest');
                result.east = this.get('mapEast');
                if (!(result.north && result.south && result.west && result.east)) {
                    result = converter.USNGtoLL(this.get('usngbb'));

                }
                this.set(result);
            },

            setFilterBBox: function (model) {
                var north = parseFloat(model.get('north'));
                var south = parseFloat(model.get('south'));
                var west = parseFloat(model.get('west'));
                var east = parseFloat(model.get('east'));

                model.set({mapNorth: north, mapSouth: south, mapEast: east, mapWest: west});
            },

            setBboxLatLon: function () {
                var north = this.get('north'),
                    south = this.get('south'),
                    west = this.get('west'),
                    east = this.get('east');
                if (north && south && east && west) {
                    var usngsStr = converter.LLBboxtoUSNG(north, south, east, west);

                    this.set('usngbb', usngsStr, {silent: this.get('locationType') !== 'usng'});
                    if (this.get('locationType') === 'usng' && this.drawing) {
                        this.repositionLatLon();
                    }
                }
            },

            setRadiusLatLon: function () {
                var lat = this.get('lat'),
                    lon = this.get('lon');
                if (lat && lon) {
                    var usngsStr = converter.LLtoUSNG(lat, lon, 5);
                    this.set('usng', usngsStr, {silent: true});
                }
            },

            setBboxUsng: function () {
                var result = converter.USNGtoLL(this.get('usngbb'));
                var newResult = {};
                newResult.mapNorth = result.north;
                newResult.mapSouth = result.south;
                newResult.mapEast = result.east;
                newResult.mapWest = result.west;
                this.set(newResult);
            },

            setBBox: function () {

                //we need these to always be inferred
                //as numeric values and never as strings
                var north = parseFloat(this.get('north'));
                var south = parseFloat(this.get('south'));
                var west = parseFloat(this.get('west'));
                var east = parseFloat(this.get('east'));

                if (north && south && east && west) {
                    this.set('bbox', [west, south, east, north].join(','), {silent: this.get('locationType') === 'usng' && !this.drawing});
                }
                if (this.get('locationType') !== 'usng') {
                    this.set({mapNorth: north, mapSouth: south, mapEast: east, mapWest: west});
                }
            },

            setRadiusUsng: function () {
                var result = converter.USNGtoLL(this.get('usng'), true);
                this.set(result);
            },

            getValue: function (value) {
                switch (typeof value) {
                    case 'string':
                        return "'" + value.replace(/'/g, "''") + "'";
                    case 'number':
                        return String(value);
                    case 'object':
                        if (_.isDate(value)) {
                            return moment.utc(value).format(properties.CQL_DATE_FORMAT);
                        } else {
                            throw new Error("Can't write object to CQL: " + value);
                        }
                        break;
                    default:
                        throw new Error("Can't write value to CQL: " + value);
                }
            },

            startScheduledSearch: function () {
                var model = this;
                if (this.get('scheduled')) {
                    var scheduleDelay = this.getScheduleDelay();
                    this.stopScheduledSearch();
                    this.timeoutId = setInterval(function () {
                        model.startSearch();
                    }, scheduleDelay);
                } else {
                    this.stopScheduledSearch();
                }
            },

            stopScheduledSearch: function () {
                if (this.timeoutId) {
                    clearInterval(this.timeoutId);
                }
            },

            getScheduleDelay: function () {
                var val;
                switch (this.get('scheduleUnits')) {
                    case 'minutes':
                        val = (this.get('scheduleValue') || 5) * 60 * 1000;
                        break;
                    case 'hours':
                        val = (this.get('scheduleValue') || 1) * 60 * 60 * 1000;
                        break;
                }
                return val;
            },

            clearSearch: function () {
                if (this.get('result')) {
                    this.get('result').cleanup();
                }
                this.set({result: undefined});
                this.trigger('searchCleared');
            },

            buildSearchData: function(){
                var data = this.toJSON();

                switch (data.federation) {
                    case 'local':
                        data.src = [Sources.localCatalog];
                        break;
                    case 'enterprise':
                        data.src = _.pluck(Sources.toJSON(), 'id');
                        break;
                    case 'selected':
                        // already in correct format
                        break;
                }

                data.sort = this.get('sortField') + ':' + this.get('sortOrder');

                return _.pick(data, 'src', 'start', 'count', 'timeout', 'cql', 'sort', 'id');
            },

            startSearch: function (options) {
                options = _.extend({
                    limitToDeleted: false,
                    limitToHistoric: false
                }, options);
                this.cancelCurrentSearches();

                var data = Common.duplicate(this.buildSearchData());
                var sources = data.src;
                var initialStatus = sources.map(function (src) {
                    return {
                        id: src
                    };
                });
                var result;
                if (this.get('result') && this.get('result').get('results')) {
                    result = this.get('result');
                    result.setColor(this.getColor());
                    result.setQueryId(this.getId());
                    result.set('merged', true);
                    result.get('mergedResults').fullCollection.reset();
                    result.get('mergedResults').reset();
                    result.get('results').fullCollection.reset();
                    result.get('results').reset();
                    result.get('status').reset(initialStatus);
                } else {
                    result = new Metacard.SearchResult({
                        queryId: this.getId(),
                        color: this.getColor(),
                        status: initialStatus
                    });
                    this.set({result: result});
                }

                var sortField = this.get('sortField');
                var sortOrder = this.get('sortOrder') === 'desc' ? -1 : 1;

                switch (sortField) {
                    case 'RELEVANCE':
                        result.get('results').fullCollection.comparator = function (a, b) {
                            return sortOrder * (a.get('relevance') - b.get('relevance'));
                        };
                        break;
                    case 'DISTANCE':
                        result.get('results').fullCollection.comparator = function (a, b) {
                            return sortOrder * (a.get('distance') - b.get('distance'));
                        };
                        break;
                    default:
                        result.get('results').fullCollection.comparator = function (a, b) {
                            var aVal = a.get('metacard>properties>' + sortField);
                            var bVal = b.get('metacard>properties>' + sortField);
                            if (aVal < bVal) {
                                return sortOrder * -1;
                            }
                            if (aVal > bVal) {
                                return sortOrder;
                            }
                            return 0;
                        };
                }

                result.set('initiated', Date.now());
                result.get('results').fullCollection.sort();

                if (sources.length === 0){
                    announcement.announce({
                        title: 'Search "'+ this.get('title') + '" cannot be run.',
                        message: 'No sources are currently selected.  Edit the search and select at least one source.',
                        type: 'warn'
                    });
                    return [];
                }

                if (!properties.isCacheDisabled) {
                    sources.unshift("cache");
                }

                var cqlString = data.cql;
                if (options.limitToDeleted) {
                    cqlString = limitToDeleted(cqlString);
                } else if (options.limitToHistoric) {
                    cqlString = limitToHistoric(cqlString);
                }
                this.currentSearches = sources.map(function (src) {
                    data.src = src;

                    // since the "cache" source will return all cached results, need to
                    // limit the cached results to only those from a selected source
                    data.cql = (src === 'cache') ?
                        CacheSourceSelector.trimCacheSources(cqlString, sources) :
                        cqlString;
                    var payload = JSON.stringify(data);

                    return result.fetch({
                        customErrorHandling: true,
                        data: payload,
                        remove: false,
                        dataType: "json",
                        contentType: "application/json",
                        method: "POST",
                        processData: false,
                        timeout: properties.timeout,
                        success: function(model, resp, options) {
                            if (options.resort === true){
                                model.get('results').fullCollection.sort();
                            }
                        },
                        error: function (model, response, options) {
                            var srcStatus = result.get('status').get(src);
                            if (srcStatus) {
                                srcStatus.set({
                                    successful: false,
                                    pending: false
                                });
                            }
                            response.options = options;
                        }
                    });
                });
                return this.currentSearches;
            },

            currentSearches: [],

            cancelCurrentSearches: function(){
                this.currentSearches.forEach(function(request){
                    request.abort();
                });
                this.currentSearches = [];
            },

            setSources: function (sources) {
                var sourceArr = [];
                sources.each(function (src) {
                    if (src.get('available') === true) {
                        sourceArr.push(src.get('id'));
                    }
                });
                if (sourceArr.length > 0) {
                    this.set('src', sourceArr.join(','));
                } else {
                    this.set('src', '');
                }
            },

            setDefaults: function () {
                var model = this;
                _.each(_.keys(model.defaults), function (key) {
                    model.set(key, model.defaults[key]);
                });
            },

            getId: function () {
                if (this.get('id')) {
                    return this.get('id');
                } else {
                    var id = this._cloneOf || this.id || Common.generateUUID();
                    this.set('id');
                    return id;
                }
            },
            setColor: function (color) {
                this.set('color', color);
            },
            getColor: function () {
                return this.get('color');
            },
            color: function () {
                return this.get('color');
            }
        });
        return Query;

    });
