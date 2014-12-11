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
        'js/model/Metacard',
        'usngs',
        'js/model/Filter',
        'wreqr',
        'backboneassociations'
    ],
    function (Backbone, _, properties, moment, Metacard, usngs, Filter,wreqr) {
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
            defaults: {
                offsetTimeUnits: 'hours',
                scheduleUnits: 'minutes',
                timeType: 'modified',
                radiusUnits: 'meters',
                radius: 0,
                radiusValue: 0,
                count: properties.resultCount,
                start: 1,
                format: "geojson",
                locationType: 'latlon',
                lat: undefined,
                lon: undefined,
                federation: 'enterprise',
                sortField: 'modified',
                sortOrder: 'desc'
            },

            drawing: false,

            initialize: function () {
                _.bindAll(this);
                this.listenTo(this, 'change:north change:south change:east change:west',this.setBBox);
                this.listenTo(this, 'change:scheduled change:scheduleValue change:scheduleUnits', this.startScheduledSearch);
                this.listenTo(this, 'change:north change:south change:east change:west', this.setBboxLatLon);
                this.listenTo(this, 'change:lat change:lon', this.setRadiusLatLon);
                this.listenTo(this, 'change:usngbb', this.setBboxUsng);
                this.listenTo(this, 'change:usng', this.setRadiusUsng);
                this.listenTo(this, 'EndExtent', this.notDrawing);
                this.listenTo(this, 'BeginExtent', this.drawingOn);

                this.filters = new Filter.Collection();

                if (this.get('scheduled')) {
                    this.startSearch();
                }

                this.startScheduledSearch();
            },

            notDrawing: function() {
                this.drawing = false;
            },

            drawingOn: function() {
                this.drawing = true;
            },

            repositionLatLon: function () {
                if (this.get('usngbb')) {
                    var result = converter.USNGtoLL(this.get('usngbb'));
                    this.set(result);
                }
            },

            setBboxLatLon: function () {
                var usngsStr = converter.LLBboxtoUSNG(this.get('north'), this.get('south'), this.get('east'),this.get('west'));

                this.set('usngbb', usngsStr, {silent:this.get('locationType') !== 'usng'});
                if (this.get('locationType') === 'usng' && this.drawing) {
                    this.repositionLatLon();
                }
            },

            setRadiusLatLon: function () {
                var usngsStr = converter.LLtoUSNG(this.get('lat'), this.get('lon'), 5);
                this.set('usng', usngsStr, {silent:true});
            },

            setBboxUsng: function () {
                var result = converter.USNGtoLL(this.get('usngbb'));
                this.set(result, {silent:this.get('locationType') === 'usng' && this.drawing});
            },

            setRadiusUsng: function () {
                var result = converter.USNGtoLL(this.get('usng'), true);
                this.set(result);
            },

            toFilters: function(){
                var filters = [];

                // contextual
                var q = this.get('q');
                if (q) {
                    filters.push(new Filter.Model({
                        fieldName: 'anyText',
                        fieldType: 'string',
                        fieldOperator: 'contains',
                        stringValue1: q
                    }));
                }

                // temporal
                var start = this.get('dtstart'),
                    end = this.get('dtend'),
                    offset = this.get('dtoffset'),
                    timeType = this.get('timeType');
                if (start && end) {
                    filters.push(new Filter.Model({
                        fieldName: timeType,
                        fieldType: 'date',
                        fieldOperator: 'after',
                        dateValue1: start
                    }));
                    filters.push(new Filter.Model({
                        fieldName: timeType,
                        fieldType: 'date',
                        fieldOperator: 'before',
                        dateValue1: end
                    }));
                } else if (start) {
                    filters.push(new Filter.Model({
                        fieldName: timeType,
                        fieldType: 'date',
                        fieldOperator: 'after',
                        dateValue1: start
                    }));
                } else if (end) {
                    filters.push(new Filter.Model({
                        fieldName: timeType,
                        fieldType: 'date',
                        fieldOperator: 'before',
                        dateValue1: end
                    }));

                } else if (offset) {
                    filters.push(new Filter.Model({
                        fieldName: timeType,
                        fieldType: 'date',
                        fieldOperator: 'after',
                        dateValue1: moment().subtract(offset, 'milliseconds').toDate()
                    }));
                }

                // spacial stuff.
                var north = this.get('north'),
                    south = this.get('south'),
                    west = this.get('west'),
                    east = this.get('east'),
                    lat = this.get('lat'),
                    lon = this.get('lon'),
                    radius = this.get('radius'),
                    polygon = this.get('polygon');
                if (north && south && east && west) {
                    filters.push(new Filter.Model({
                        fieldName: 'anyGeo',
                        fieldType: 'anyGeo',
                        fieldOperator: 'intersects',
                        geoType: 'bbox',
                        north: north,
                        south: south,
                        west: west,
                        east: east
                    }));

                } else if (polygon) {
                    filters.push(new Filter.Model({
                        fieldName: 'anyGeo',
                        fieldType: 'anyGeo',
                        fieldOperator: 'intersects',
                        geoType: 'polygon',
                        polygon: polygon
                    }));
                }else if (lat && lon && radius) {
                    filters.push(new Filter.Model({
                        fieldName: 'anyGeo',
                        fieldType: 'anyGeo',
                        fieldOperator: 'intersects',
                        geoType: 'circle',
                        lon: lon,
                        lat: lat,
                        radius: radius
                    }));
                }

                // if no filters so far, lets create a global search one.
                if (_.isEmpty(filters)) {
                    filters.push(new Filter.Model({
                        fieldName: 'anyText',
                        fieldType: 'string',
                        fieldOperator: 'contains',
                        stringValue1: '%'
                    }));
                }

                // type
                var types = this.get('type');
                if (types) {
                    filters.push(new Filter.Model({
                        fieldName: properties.filters.METADATA_CONTENT_TYPE,
                        fieldType: 'string',
                        fieldOperator: 'contains',
                        stringValue1: types  // this should already be common delimited for us.
                    }));
                } else {
                    // fill in all content types and the no value type.
                    var contentTypes = wreqr.reqres.request('workspace:gettypes');
                    var allTypes = contentTypes.pluck('name');
                    allTypes.push('no-value');
                    filters.push(new Filter.Model({
                        fieldName: properties.filters.METADATA_CONTENT_TYPE,
                        fieldType: 'string',
                        fieldOperator: 'contains',
                        stringValue1: allTypes.join(',')
                    }));
                }

                var src = this.get('src');
                if(src){
                    filters.push(new Filter.Model({
                        fieldName: properties.filters.SOURCE_ID,
                        fieldType: 'string',
                        fieldOperator: 'equals',
                        stringValue1: src
                    }));
                }

                return filters;
            },

            getCql: function () {
                var filters = [];

                // contextual
                var q = this.get('q');
                if (q) {
                    filters.push('anyText ILIKE ' + this.getValue(q));
                }

                // temporal
                var start = this.get('dtstart'),
                    end = this.get('dtend'),
                    offset = this.get('dtoffset'),
                    timeType = this.get('timeType');
                if (start && end) {
                    filters.push(timeType + ' DURING ' + this.getValue(new Date(start)) + '/' +
                        this.getValue(new Date(end)));
                } else if (start) {
                    filters.push(timeType + ' AFTER ' + this.getValue(new Date(start)));
                } else if (end) {
                    filters.push(timeType + ' BEFORE ' + this.getValue(new Date(end)));
                } else if (offset) {
                    filters.push(timeType + ' AFTER ' +
                        moment().subtract(offset, 'milliseconds').format(properties.CQL_DATE_FORMAT));
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
                    var bbox = 'POLYGON ((' +
                        west + ' ' + south +
                        ', ' + west + ' ' + north +
                        ', ' + east + ' ' + north +
                        ', ' + east + ' ' + south +
                        ', ' + west + ' ' + south +
                        '))';
                    filters.push('INTERSECTS(anyGeo, ' + bbox + ')');
                } else if (polygon) {
                    var poly = 'POLYGON ((';
                    var polyPoint;
                    for (var i = 0;i<polygon.length;i++) {
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

            getValue: function(value) {
                switch (typeof value) {
                    case 'string':
                        return "'" + value.replace(/'/g, "''") + "'";
                    case 'number':
                        return String(value);
                    case 'object':
                        if (_.isDate(value)) {
                            return moment(value).format(properties.CQL_DATE_FORMAT);
                        } else {
                            throw new Error("Can't write object to CQL: " + value);
                        }
                        break;
                    default:
                        throw new Error("Can't write value to CQL: " + value);
                }
            },

            toJSON: function() {
                var json = _.clone(this.attributes);

                json.cql = this.getCql();

                return json;
            },

            startScheduledSearch: function() {
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

            stopScheduledSearch: function() {
                if (this.timeoutId) {
                    clearInterval(this.timeoutId);
                }
            },

            getScheduleDelay: function() {
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

            clearSearch: function() {
                if (this.get('result')) {
                    this.get('result').cleanup();
                }
                this.set({result: undefined});
                this.filters.reset(this.toFilters());
                this.trigger('searchCleared');
            },


            buildSearchData: function(){
                var data = this.toJSON();
                if(this.filters.length === 0){
                    this.filters.reset(this.toFilters()); // init filters from search parameters.
                }
                // this overrides the cql generation with the filters cql.
                data.cql = this.filters.toCQL();

                // lets handle the source-id filters since they are not included in the cql.
                var sourceFilters = this.filters.where({fieldName: properties.filters.SOURCE_ID});
                var sources = [];
                _.each(sourceFilters, function(sourceFilter){
                    sources.push(sourceFilter.get('stringValue1'));
                });
                data.src = sources.join(',');

                data.sort = this.get('sortField') + ':' + this.get('sortOrder');

                return data;
            },

            startSearch:function(progressFunction) {

                var result;
                if (this.get('result')) {
                    result = this.get('result');
                } else {
                    result = new Metacard.SearchResult();
                    this.set({result: result});
                }
                
                result.set('initiated', moment().format('lll'));

                var progress = progressFunction || function() {
                    var localResult = result;
                    localResult.get('results').each(function(searchResult) {
                        searchResult.cleanup();
                    });
                    localResult.mergeLatest();
                    localResult = null;
                };

                var data = this.buildSearchData();

                return result.fetch({
                    progress: progress,
                    data: data,
                    dataType: "json",
                    timeout: 300000,
                    error : function(){
                        if (typeof console !== 'undefined') {
                            console.error(arguments);
                        }
                    }
                });
            },

            setSources: function(sources) {
                var sourceArr = [];
                sources.each(function (src) {
                    if (src.get('available') === true) {
                        sourceArr.push(src.get('id'));
                    }
                });
                if (sourceArr.length > 0) {
                    this.set('src', sourceArr.join(','));
                }
            },

            setDefaults : function() {
                var model = this;
                _.each(_.keys(model.defaults), function(key) {
                    model.set(key, model.defaults[key]);
                });
            },

            setBBox : function() {
                var north = this.get('north'),
                    south = this.get('south'),
                    west = this.get('west'),
                    east = this.get('east');
                if (north && south && east && west){
                    this.set('bbox', [west,south,east,north].join(','));
                }
            },

            swapDatesIfNeeded : function() {
                var model = this;
                if (model.get('dtstart') && model.get('dtend')){
                    var start = new Date(model.get('dtstart'));
                    var end = new Date(model.get('dtend'));
                    if (start > end){
                        this.set({
                            dtstart : end.toISOString(),
                            dtend : start.toISOString()
                        });
                    }
                }
            }
        });
        return Query;

    });
