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
        'backboneassociations'
    ],
    function (Backbone, _, properties, moment, cql, wellknown, Metacard, Sources, usngs, wreqr, Common) {
        "use strict";
        var Query = {};

        var converter = new usngs.Converter();

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
                    cql: "anyText ILIKE '%'",
                    title: 'Untitled',
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

            parse: function (resp) {
                if (resp.cql) {
                    this.filter = cql.read(resp.cql);
                    _.extend(resp, this.defaultsFromFilter(this.filter, {}));
                }
                return resp;
            },

            defaultsFromFilter: function (filter, memo) {
                var defaultValues = memo;
                switch (filter.type) {
                    case 'BBOX':
                        if (filter.property === 'anyGeo') {
                            var xmin = filter.value[0],
                                ymin = filter.value[1],
                                xmax = filter.value[2],
                                ymax = filter.value[3];
                            _.extend(defaultValues, {
                                north: ymax,
                                south: ymin,
                                west: xmin,
                                east: xmax,
                                bbox: [xmin, ymin, xmax, ymax]
                            });
                        }
                        break;
                    case 'DWITHIN':
                        if (filter.property === 'anyGeo') {
                            var geo = wellknown.parse(filter.value.value);
                            if (geo.geometry.type === 'Point') {
                                _.extend(defaultValues, {
                                    lon: geo.geometry.coordinates[0],
                                    lat: geo.geometry.coordinates[1],
                                    radius: filter.distance
                                });
                            }
                        }
                        break;
                    case 'INTERSECTS':
                        if (filter.property === 'anyGeo') {
                            var polygon = wellknown.parse(filter.value.value);
                            _.extend(defaultValues, {
                                polygon: polygon.coordinates[0]
                            });
                        }
                        break;
                    case 'AND':
                    case 'OR':
                        for (var i = 0; i < filter.filters.length; i++) {
                            _.extend(defaultValues, this.defaultsFromFilter(filter.filters[i], defaultValues));
                        }
                        break;
                    case 'LIKE':
                    case 'ILIKE':
                        if (filter.property === 'anyText') {
                            var matchcase = filter.type === 'LIKE';
                            _.extend(defaultValues, {
                                q: filter.value,
                                matchcase: matchcase
                            });
                        }
                        // TODO content types?
                        break;
                    case 'BEFORE':
                        _.extend(defaultValues, {
                            dtend: filter.value,
                            timeType: filter.property
                        });
                        break;
                    case 'AFTER':
                        _.extend(defaultValues, {
                            dtstart: filter.value,
                            timeType: filter.property
                        });
                        break;
                    default:
                }
                return defaultValues;
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

            toFilters: function () {
                var filters = [];

                // contextual
                var q = this.get('q');
                if (q) {
                    if (this.get('matchcase')) {
                        filters.push({
                            type: 'LIKE',
                            property: 'anyText',
                            value: q
                        });
                    } else {
                        filters.push({
                            type: 'ILIKE',
                            property: 'anyText',
                            value: q
                        });
                    }
                }

                // temporal
                var start = this.get('dtstart'),
                    end = this.get('dtend'),
                    offset = this.get('dtoffset'),
                    timeType = this.get('timeType');
                if (start && end) {
                    filters.push({
                        type: 'AFTER',
                        property: timeType,
                        value: start
                    });
                    filters.push({
                        type: 'BEFORE',
                        property: timeType,
                        value: end
                    });
                } else if (start) {
                    filters.push({
                        type: 'AFTER',
                        property: timeType,
                        value: start
                    });
                } else if (end) {
                    filters.push({
                        type: 'BEFORE',
                        property: timeType,
                        value: end
                    });
                } else if (offset) {
                    filters.push({
                        type: 'AFTER',
                        property: timeType,
                        value: moment().subtract(offset, 'milliseconds').toDate()
                    });
                }

                // spatial stuff.
                var north = this.get('north'),
                    south = this.get('south'),
                    west = this.get('west'),
                    east = this.get('east'),
                    lat = this.get('lat'),
                    lon = this.get('lon'),
                    radius = this.get('radius'),
                    polygon = this.get('polygon');
                if (north && south && east && west) {
                    filters.push({
                        type: 'BBOX',
                        property: 'anyGeo',
                        value: [west, south, east, north]
                    });
                } else if (polygon) {
                    filters.push({
                        type: 'INTERSECTS',
                        property: 'anyGeo',
                        value: polygon
                    });
                } else if (lat && lon && radius) {
                    filters.push({
                        type: 'DWITHIN',
                        property: 'anyGeo',
                        value: 'POINT(' + lon + ' ' + lat + ')',
                        distance: Number(radius)
                    });
                }

                // if no filters so far, lets create a global search one.
                if (_.isEmpty(filters)) {
                    filters.push({
                        type: 'ILIKE',
                        property: 'anyText',
                        value: '%'
                    });
                }

                // type
                var types = this.get('type');
                if (types) {
                    filters.push({
                        type: 'ILIKE',
                        property: properties.filters.METADATA_CONTENT_TYPE,
                        value: types // this should already be common delimited for us.
                    });
                }

                var src = this.get('src');
                if (src) {
                    filters.push({
                        type: '=',
                        property: properties.filters.SOURCE_ID,
                        value: src
                    });
                }

                return filters;
            },

            getCql: function () {
                var filters = [];

                // contextual
                var q = this.get('q');
                if (q) {
                    var likeOp = this.get('matchcase') ? ' LIKE ' : ' ILIKE ';
                    filters.push('anyText' + likeOp + this.getValue(q));
                }

                // temporal
                var start = this.get('dtstart'),
                    end = this.get('dtend'),
                    offset = this.get('dtoffset'),
                    timeType = this.get('timeType');
                if (start && end) {
                    filters.push(timeType + ' AFTER ' + this.getValue(new Date(start)));
                    filters.push(timeType + ' BEFORE ' + this.getValue(new Date(end)));
                } else if (start) {
                    filters.push(timeType + ' AFTER ' + this.getValue(new Date(start)));
                } else if (end) {
                    filters.push(timeType + ' BEFORE ' + this.getValue(new Date(end)));
                } else if (offset) {
                    filters.push(timeType + ' AFTER ' +
                        moment.utc().subtract(offset, 'milliseconds').format(properties.CQL_DATE_FORMAT));
                }

                // spatial
                var north = this.get('north'),
                    south = this.get('south'),
                    west = this.get('west'),
                    east = this.get('east'),
                    lat = this.get('lat'),
                    lon = this.get('lon'),
                    radius = this.get('radius'),
                    polygon = this.get('polygon');
                if (north && south && east && west) {
                    var bbox = 'POLYGON((' +
                        west + ' ' + south +
                        ', ' + west + ' ' + north +
                        ', ' + east + ' ' + north +
                        ', ' + east + ' ' + south +
                        ', ' + west + ' ' + south +
                        '))';
                    filters.push('INTERSECTS(anyGeo, ' + bbox + ')');
                } else if (polygon) {
                    var poly = 'POLYGON((';
                    var polyPoint;
                    for (var i = 0; i < polygon.length; i++) {
                        polyPoint = polygon[i];
                        poly += polyPoint[0] + ' ' + polyPoint[1];
                        if (i < polygon.length - 1) {
                            poly += ', ';
                        }
                    }
                    poly += '))';
                    filters.push('INTERSECTS(anyGeo, ' + poly + ')');
                } else if (lat && lon && radius) {
                    var point = 'POINT(' + lon + ' ' + lat + ')';
                    filters.push('DWITHIN(anyGeo, ' + point + ', ' + radius + ', meters)');
                }

                // type
                var types = this.get('type');
                if (types) {
                    var typeFilters = [];
                    _.each(types.split(','), function (type) {
                        typeFilters.push('"' + properties.filters.METADATA_CONTENT_TYPE + '" = ' + this.getValue(type));
                    }, this);

                    filters.push(this.logicalCql(typeFilters, 'OR'));
                }

                var cql = this.logicalCql(filters, 'AND');
                if (!cql) {
                    cql = "anyText LIKE '%'";
                }

                return cql;
            },
            setCql: function () {
                this.set('cql', this.getCql());
            },
            logicalCql: function (filters, operator) {
                var cql = '';
                if (filters.length === 1) {
                    cql = filters[0];
                } else if (filters.length > 1) {
                    cql = _.map(filters, function (filter) {
                        return '(' + filter + ')';
                    }).join(' ' + operator + ' ');
                }
                return cql;
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
                        data.src = ["local"];
                        break;
                    case 'enterprise':
                        data.src = _.pluck(Sources.where({'available': true}), 'id');
                        break;
                    case 'selected':
                        // already in correct format
                        break;
                }

                data.sort = this.get('sortField') + ':' + this.get('sortOrder');

                return _.pick(data, 'src', 'start', 'count', 'timeout', 'cql', 'sort', 'id');
            },

            startSearch: function () {
                this.cancelCurrentSearches();

                var data = this.buildSearchData();
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
                    result.get('results').fullCollection.reset();
                    result.get('results').reset();
                    result.get('status').reset(initialStatus);
                } else {
                    result = new Metacard.SearchResult({
                        queryId: this.getId(),
                        color: this.getColor(),
                        status: initialStatus
                    });
                    result.get('results').state.pageSize = 50;
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

                result.set('initiated', moment().format('lll'));
                result.get('results').fullCollection.sort();

                sources.unshift("cache");
                this.currentSearches = sources.map(function (src) {
                    data.src = src;
                    return result.fetch({
                        data: JSON.stringify(data),
                        remove: false,
                        dataType: "json",
                        contentType: "application/json",
                        method: "POST",
                        processData: false,
                        timeout: properties.timeout,
                        success: function() {
                            var length = result.get('results').fullCollection.length;
                            result.get('results').state.totalRecords = length;
                            var totalPages = Math.ceil(length / result.get('results').state.pageSize);
                            result.get('results').state.totalPages = totalPages;
                            result.get('results').state.lastPage = totalPages;

                            result.get('results').fullCollection.sort();
                        },
                        error: function () {
                            var srcStatus = result.get('status').get(src);
                            if (srcStatus) {
                                srcStatus.set({
                                    successful: false,
                                    pending: false
                                });
                            }
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

            swapDatesIfNeeded: function () {
                var model = this;
                if (model.get('dtstart') && model.get('dtend')) {
                    var start = new Date(model.get('dtstart'));
                    var end = new Date(model.get('dtend'));
                    if (start > end) {
                        this.set({
                            dtstart: end.toISOString(),
                            dtend: start.toISOString()
                        });
                    }
                }
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
