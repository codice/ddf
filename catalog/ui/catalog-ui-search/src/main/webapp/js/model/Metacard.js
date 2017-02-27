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
/*global define, setTimeout*/
/*jshint loopfunc: true, latedef: nofunc */
define([
        'backbone',
        'underscore',
        'jquery',
        'wreqr',
        'component/singletons/metacard-definitions',
        'component/singletons/sources-instance',
        'terraformer',
        'terraformer-wkt-parser',
        'js/CQLUtils',
        '@turf/turf',
        'moment',
        'backboneassociations',
        'backbone.paginator'
    ],
    function (Backbone, _, $, wreqr, metacardDefinitions, Sources, Terraformer, TerraformerWKTParser, CQLUtils,
              Turf, moment) {
        "use strict";

        var blacklist = [];

        function generateThumbnailUrl(url){
            var newUrl = url;
            if(url.indexOf("?") >= 0) {
                newUrl += '&';
            } else {
                newUrl += '?';
            }
            newUrl += '_=' +Date.now();
            return newUrl;
        }


        function checkTokenWithWildcard(token, filter){
            var filterRegex = new RegExp(filter.split('*').join('.*'));
            return filterRegex.test(token);
        }

        function checkToken(token, filter){
            if (filter.indexOf('*') >= 0){
                return checkTokenWithWildcard(token, filter);
            } else if (token === filter){
                return true;
            }
            return false;
        }

        function matchesILIKE(value, filter){
            var valueToCheckFor = filter.value.toLowerCase();
            value = value.toString().toLowerCase();
            var tokens = value.split(' ');
            for (var i = 0; i <= tokens.length - 1; i++){
                if (checkToken(tokens[i], valueToCheckFor)){
                    return true;
                }
            }
            return false;
        }

        function matchesLIKE(value, filter){
            var valueToCheckFor = filter.value;
            var tokens = value.toString().split(' ');
            for (var i = 0; i <= tokens.length - 1; i++){
                if (checkToken(tokens[i], valueToCheckFor)){
                    return true;
                }
            }
            return false;
        }

        function matchesEQUALS(value, filter) {
            var valueToCheckFor = filter.value;
            if (value.toString() === valueToCheckFor.toString()) {
                return true;
            }
            return false;
        }

        function matchesNOTEQUALS(value, filter) {
            var valueToCheckFor = filter.value;
            if (value.toString() !== valueToCheckFor.toString()) {
                return true;
            }
            return false;
        }

        function matchesGreaterThan(value, filter){
            var valueToCheckFor = filter.value;
            if (value > valueToCheckFor){
                return true;
            }
            return false;
        }

        function matchesGreaterThanOrEqualTo(value, filter){
            var valueToCheckFor = filter.value;
            if (value >= valueToCheckFor){
                return true;
            }
            return false;
        }

        function matchesLessThan(value, filter){
            var valueToCheckFor = filter.value;
            if (value < valueToCheckFor){
                return true;
            }
            return false;
        }

        function matchesLessThanOrEqualTo(value, filter){
            var valueToCheckFor = filter.value;
            if (value <= valueToCheckFor){
                return true;
            }
            return false;
        }

        // terraformer doesn't offically support Point, MultiPoint, FeatureCollection, or GeometryCollection
        // terraformer incorrectly supports MultiPolygon, so turn it into a Polygon first
        function intersects(terraformerObject, value){
            var intersected = false;
            switch(value.type){
                case 'Point':
                    return terraformerObject.contains(value);
                case 'MultiPoint':
                    value.coordinates.forEach(function(coordinate){
                        intersected = intersected || intersects(terraformerObject, {
                            type: 'Point',
                            coordinates: coordinate
                        });
                    });
                    return intersected;
                case 'LineString':
                case 'MultiLineString':
                case 'Polygon':
                    return terraformerObject.intersects(value);
                case 'MultiPolygon':
                    value.coordinates.forEach(function(coordinate){
                        intersected = intersected || intersects(terraformerObject, {
                            type: 'Polygon',
                            coordinates: coordinate
                        });
                    });
                    return intersected;
                case 'Feature':
                    return intersects(terraformerObject, value.geometry);
                case 'FeatureCollection':
                    value.features.forEach(function(feature){
                        intersected = intersected || intersects(terraformerObject, feature);
                    });
                    return intersected;
                case 'GeometryCollection':
                    value.geometries.forEach(function(geometry){
                        intersected = intersected || intersects(terraformerObject, geometry);
                    });
                    return intersected;
                default:
                    return intersected;
            }
        }

        function matchesPOLYGON(value, filter){
            var polygonToCheck = TerraformerWKTParser.parse(filter.value.value);
            if (intersects(polygonToCheck, value)){
                return true;
            }
            return false;
        }

        function matchesCIRCLE(value, filter){
            if (filter.distance <= 0){
                return false;
            }
            var points = filter.value.value.substring(6, filter.value.value.length-1).split(' ');
            var circleToCheck = new Terraformer.Circle(points, filter.distance, 64);
            var polygonCircleToCheck = new Terraformer.Polygon(circleToCheck.geometry);
            if (intersects(polygonCircleToCheck, value)){
                return true;
            }
            return false;
        }

        function matchesLINESTRING(value, filter){
            var pointText = filter.value.value.substring(11);
            pointText = pointText.substring(0, pointText.length - 1);
            var lineWidth = filter.distance || 0;
            if (lineWidth <= 0){
                return false;
            }
            var line = pointText.split(',').map(function (coordinate) {
                return coordinate.split(' ').map(function (value) {
                    return Number(value);
                });
            });
            var turfLine = Turf.lineString(line);
            var bufferedLine = Turf.buffer(turfLine, lineWidth, 'meters');
            var polygonToCheck = new Terraformer.Polygon({
                type: 'Polygon',
                coordinates: bufferedLine.geometry.coordinates
            });
            if (intersects(polygonToCheck, value)){
                return true;
            }
            return false;
        }

        function matchesBEFORE(value, filter){
            var date1 = moment(value);
            var date2 = moment(filter.value);
            if (date1 <= date2){
                return true;
            }
            return false;
        }

        function matchesAFTER(value, filter){
            var date1 = moment(value);
            var date2 = moment(filter.value);
            if (date1 >= date2){
                return true;
            }
            return false;
        }

        function flattenMultivalueProperties(valuesToCheck){
            return _.flatten(valuesToCheck, true);
        }

        function matchesFilter(metacard, filter, metacardTypes) {
            if (!filter.filters) {
                var valuesToCheck = [];
                if (metacardTypes[filter.property] && metacardTypes[filter.property].type === 'GEOMETRY') {
                    filter.property = 'anyGeo';
                }
                switch (filter.property) {
                    case '"anyText"':
                        valuesToCheck = Object.keys(metacard.properties).filter(function (property) {
                            return blacklist.indexOf(property) === -1;
                        }).filter(function (property) {
                            return Boolean(metacardTypes[property]) && (metacardTypes[property].type === 'STRING');
                        }).map(function (property) {
                            return metacard.properties[property];
                        });
                        break;
                    case 'anyGeo':
                        valuesToCheck = Object.keys(metacard.properties).filter(function (property) {
                            return blacklist.indexOf(property) === -1;
                        }).filter(function (property) {
                            return Boolean(metacardTypes[property]) && (metacardTypes[property].type === 'GEOMETRY');
                        }).map(function (property) {
                            return new Terraformer.Primitive(metacard.properties[property]);
                        });
                        if (metacard.geometry) {
                            valuesToCheck.push(new Terraformer.Primitive(metacard.geometry));
                        }
                        break;
                    default:
                        var valueToCheck = metacard.properties[filter.property.replace(/['"]+/g, '')];
                        if (valueToCheck !== undefined) {
                            valuesToCheck.push(valueToCheck);
                        }
                        break;
                }

                if (valuesToCheck.length === 0) {
                    return false;
                }

                valuesToCheck = flattenMultivalueProperties(valuesToCheck);

                for (var i = 0; i <= valuesToCheck.length - 1; i++) {
                    switch (filter.type) {
                        case 'ILIKE':
                            if (matchesILIKE(valuesToCheck[i], filter)){
                                return true;
                            }
                            break;
                        case 'LIKE':
                            if (matchesLIKE(valuesToCheck[i], filter)) {
                                return true;
                            }
                            break;
                        case '=':
                            if (matchesEQUALS(valuesToCheck[i], filter)) {
                                return true;
                            }
                            break;
                        case '!=':
                            if (matchesNOTEQUALS(valuesToCheck[i], filter)) {
                                return true;
                            }
                            break;
                        case '>':
                            if (matchesGreaterThan(valuesToCheck[i], filter)) {
                                return true;
                            }
                            break;
                        case '>=':
                            if (matchesGreaterThanOrEqualTo(valuesToCheck[i], filter)) {
                                return true;
                            }
                            break;
                        case '<':
                            if (matchesLessThan(valuesToCheck[i], filter)) {
                                return true;
                            }
                            break;
                        case '<=':
                            if (matchesLessThanOrEqualTo(valuesToCheck[i], filter)) {
                                return true;
                            }
                            break;
                        case 'INTERSECTS':
                            if (matchesPOLYGON(valuesToCheck[i], filter)) {
                                return true;
                            }
                            break;
                        case 'DWITHIN':
                            if (CQLUtils.isPointRadiusFilter(filter)){
                                if (matchesCIRCLE(valuesToCheck[i], filter)){
                                    return true;
                                }
                            } else if (matchesLINESTRING(valuesToCheck[i], filter)){
                                return true;
                            }
                            break;
                        case 'AFTER':
                            if (matchesAFTER(valuesToCheck[i], filter)) {
                                return true;
                            }
                            break;
                        case 'BEFORE':
                            if (matchesBEFORE(valuesToCheck[i], filter)) {
                                return true;
                            }
                            break;
                    }
                }
                return false;
            } else {
                return matchesFilters(metacard, filter, metacardTypes);
            }
        }

        function matchesFilters(metacard, resultFilter, metacardTypes) {
            switch (resultFilter.type) {
                case 'AND':
                    for (var i = 0; i <= resultFilter.filters.length - 1; i++) {
                        if (!matchesFilter(metacard, resultFilter.filters[i], metacardTypes)) {
                            return false;
                        }
                    }
                    return true;
                case 'OR':
                    for (var j = 0; j <= resultFilter.filters.length - 1; j++) {
                        if (matchesFilter(metacard, resultFilter.filters[j], metacardTypes)) {
                            return true;
                        }
                    }
                    break;
                default:
                    return matchesFilter(metacard, resultFilter, metacardTypes);
            }
        }

        function parseMultiValue(value){
            if (value && value.constructor === Array){
                return value[0];
            }
            return value;
        }

        function isEmpty(value){
            return value === undefined || value === null;
        }

        function parseValue(value, attribute){
            var attributeDefinition = metacardDefinitions.metacardTypes[attribute];
            if (!attributeDefinition) {
                return value.toString().toLowerCase();
            }
            switch(attributeDefinition.type){
                case 'DATE':
                case 'BOOLEAN':
                    return value;
                case 'STRING':
                    return value.toString().toLowerCase();
                default:
                    return parseFloat(value);
            }
        }

        function compareValues(aVal, bVal, sorting) {
            var sortOrder = sorting.direction === 'descending' ? -1 : 1;
            aVal = parseValue(aVal, sorting.attribute);
            bVal = parseValue(bVal, sorting.attribute);
            if (aVal < bVal) {
                return sortOrder * -1;
            }
            if (aVal > bVal) {
                return sortOrder;
            }
            return 0;
        }

        function checkSortValue(a, b, sorting){
            var aVal = parseMultiValue(a.get('metacard>properties>' + sorting.attribute));
            var bVal = parseMultiValue(b.get('metacard>properties>' + sorting.attribute));
            if (isEmpty(aVal)){
                return 1;
            }
            if (isEmpty(bVal)){
                return -1;
            }
            return compareValues(aVal, bVal, sorting);
        }

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
            defaults: function() {
                return {
                    'metacard-tags': ['resource']
               }
           }
        });

        MetaCard.Action = Backbone.AssociatedModel.extend({

        });

        MetaCard.Metacard = Backbone.AssociatedModel.extend({
            url: '/services/catalog/',

            initialize: function () {
                this.listenTo(wreqr.vent, 'metacard:selected', _.bind(this.onAppContext, this));
            },

            onAppContext: function (direction, model) {
                if (model.id !== this.id) {
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
            ],
            defaults: {
                'queryId': undefined
            }
        });

        MetaCard.MetacardResult = Backbone.AssociatedModel.extend({
            defaults: function(){
                return {
                    isResourceLocal: false
                };
            },
            relations: [
                {
                    type: Backbone.One,
                    key: 'metacard',
                    relatedModel: MetaCard.Metacard
                }
            ],
            initialize: function(){
                this.refreshData = _.throttle(this.refreshData, 200);
            },
            isWorkspace: function(){
                return this.get('metacard').get('properties').get('metacard-tags').indexOf('workspace') >= 0;
            },
            isResource: function(){
                return this.get('metacard').get('properties').get('metacard-tags').indexOf('resource') >= 0;
            },
            isRevision: function(){
                return this.get('metacard').get('properties').get('metacard-tags').indexOf('revision') >= 0;
            },
            isDeleted: function(){
                return this.get('metacard').get('properties').get('metacard-tags').indexOf('deleted') >= 0;
            },
            isRemote: function(){
                return this.get('metacard').get('properties').get('source-id') !== Sources.localCatalog;
            },
            refreshData: function(){
                //let solr flush
                setTimeout(function() {
                    var req = {
                        count: 1,
                        cql: CQLUtils.transformFilterToCQL({
                            type: 'AND', 
                            filters: [
                                {
                                    type: '=', 
                                    property: '"id"', 
                                    value: this.get('metacard').id
                                },
                                {
                                    type: 'ILIKE', 
                                    property: '"metacard-tags"', 
                                    value: '*'
                                }
                            ]
                        }),
                        id: '0',
                        sort: 'modified:desc',
                        src: this.get('metacard').get('properties').get('source-id'),
                        start: 1
                    };
                    $.post('/search/catalog/internal/cql', JSON.stringify(req)).then(this.parseRefresh.bind(this), this.handleRefreshError.bind(this));
                }.bind(this), 1000);
            },
            handleRefreshError: function(){
                //do nothing for now, should we announce this?
            },
            parseRefresh: function(response){
                var queryId = this.get('metacard').get('queryId');
                var color = this.get('metacard').get('color');
                _.forEach(response.results, function (result) {
                    delete result.relevance;
                    result.propertyTypes = response.types[result.metacard.properties['metacard-type']];
                    result.metacardType = result.metacard.properties['metacard-type'];
                    result.metacard.id = result.metacard.properties.id;
                    result.id = result.metacard.id + result.metacard.properties['source-id'];
                    result.metacard.queryId = queryId;
                    result.metacard.color = color;
                    var thumbnailAction = _.findWhere(result.actions, {id: 'catalog.data.metacard.thumbnail'});
                    if (result.hasThumbnail && thumbnailAction) {
                        result.metacard.properties.thumbnail = generateThumbnailUrl(thumbnailAction.url);
                    } else {
                        result.metacard.properties.thumbnail = undefined;
                    }
                });
                this.set(response.results[0]);
                this.trigger('refreshdata');
            }
        });

        MetaCard.Results = Backbone.PageableCollection.extend({
            state: {
              pageSize: 25
            },
            model: MetaCard.MetacardResult,
            mode: "client",
            amountFiltered: 0,
            generateFilteredVersion: function(filter){
                var filteredCollection = new this.constructor();
                filteredCollection.set(this.updateFilteredVersion(filter));
                filteredCollection.listenToOriginalCollection(this, filter);
                filteredCollection.amountFiltered = this.amountFiltered;
                return filteredCollection;
            },
            listenToOriginalCollection: function(originalCollection, filter){
                var debouncedUpdate = _.debounce(function(){
                    this.reset(originalCollection.updateFilteredVersion(filter));
                }.bind(this), 200);
                this.listenTo(originalCollection, 'add', debouncedUpdate);
                this.listenTo(originalCollection, 'remove',debouncedUpdate);
                this.listenTo(originalCollection, 'update', debouncedUpdate);
            },
            updateFilteredVersion: function(filter){
                this.amountFiltered = 0;
                if (filter) {
                    return this.fullCollection.filter(function (result) {
                        var passFilter = matchesFilters(result.get('metacard').toJSON(), filter, metacardDefinitions.metacardTypes);
                        if (!passFilter) {
                            this.amountFiltered++;
                        }
                        return passFilter;
                    }.bind(this));
                } else {
                    return this.fullCollection.models;
                }
            },
            updateSorting: function(sorting){
                if (sorting) {
                    this.fullCollection.comparator = function (a, b) {
                        var sortValue = 0;
                        for (var i = 0; i <= sorting.length - 1; i++) {
                            sortValue = checkSortValue(a, b, sorting[i]);
                            if (sortValue !== 0) {
                                break;
                            }
                        }
                        return sortValue;
                    };
                    this.fullCollection.sort();
                }
            },
            collapseDuplicates: function () {
                var collapsedCollection = new this.constructor();
                collapsedCollection.set(this.fullCollection.models);
                collapsedCollection.amountFiltered = this.amountFiltered;
                var endIndex = collapsedCollection.fullCollection.length;
                for (var i = 0; i < endIndex; i++) {
                    var currentResult = collapsedCollection.fullCollection.models[i];
                    var currentChecksum = currentResult.get('metacard').get('properties').get('checksum');
                    var currentId =  currentResult.get('metacard').get('properties').get('id');
                    var duplicates = collapsedCollection.fullCollection.filter(function (result) {
                        var comparedChecksum = result.get('metacard').get('properties').get('checksum');
                        var comparedId = result.get('metacard').get('properties').get('id');
                        return (result.id !== currentResult.id) && ((comparedId === currentId) ||
                            (Boolean(comparedChecksum) && Boolean(currentChecksum) && (comparedChecksum === currentChecksum)));
                    });
                    currentResult.duplicates = undefined;
                    if (duplicates.length > 0) {
                        currentResult.duplicates = duplicates;
                        collapsedCollection.fullCollection.remove(duplicates);
                        endIndex = collapsedCollection.fullCollection.length;
                    }
                }
                return collapsedCollection;
            },
            selectBetween: function(startIndex, endIndex) {
                var allModels = [];
                this.forEach(function(model){
                    allModels.push(model);
                    if (model.duplicates){
                        model.duplicates.forEach(function(duplicate){
                            allModels.push(duplicate);
                        });
                    }
                });
                return allModels.slice(startIndex, endIndex);
            }
        });

        MetaCard.SourceStatus = Backbone.AssociatedModel.extend({

        });

        MetaCard.SearchResult = Backbone.AssociatedModel.extend({
            defaults: {
                'queryId': undefined,
                'results': [],
                'mergedResults': [],
                'merged': true,
                'currentlyViewed': false
            },
            relations: [
                {
                    type: Backbone.Many,
                    key: 'mergedResults',
                    collectionType: MetaCard.Results,
                    relatedModel: MetaCard.MetacardResult
                },
                {
                    type: Backbone.Many,
                    key: 'results',
                    collectionType: MetaCard.Results,
                    relatedModel: MetaCard.MetacardResult
                },
                {
                    type: Backbone.Many,
                    key: 'status',
                    relatedModel: MetaCard.SourceStatus
                }
            ],
            url: "/search/catalog/internal/cql",
            useAjaxSync: true,
            initialize: function(){
                this.listenTo(this.get('mergedResults'), 'add change', _.throttle(this.updateMerged, 2500, {leading: false}));
                this.listenTo(this, 'change:currentlyViewed', this.handleCurrentlyViewed);
            },
            parse: function (resp, options) {
                metacardDefinitions.addMetacardDefinitions(resp.types);
                if (resp.results) {
                    var queryId = this.getQueryId();
                    var color = this.getColor();
                    _.forEach(resp.results, function (result) {
                        result.propertyTypes = resp.types[result.metacard.properties['metacard-type']];
                        result.metacardType = result.metacard.properties['metacard-type'];
                        result.metacard.id = result.metacard.properties.id;
                        result.id = result.metacard.id + result.metacard.properties['source-id'];
                        result.metacard.queryId = queryId;
                        result.metacard.color = color;

                        var thumbnailAction = _.findWhere(result.actions, {id: 'catalog.data.metacard.thumbnail'});
                        if (result.hasThumbnail && thumbnailAction) {
                           result.metacard.properties.thumbnail = generateThumbnailUrl(thumbnailAction.url);
                        }
                    });

                    if (this.allowAutoMerge()){
                        this.lastMerge = Date.now();
                        options.resort = true;
                    }
                }

                return {
                    mergedResults: resp.results,
                    results: this.allowAutoMerge() ? resp.results : [],
                    status: resp.status   
                };
            },
            allowAutoMerge: function(){
                if (this.get('results').length === 0 || !this.get('currentlyViewed')){
                    return true;
                } else {
                    return (Date.now() - this.lastMerge) < 16;
                }
            },
            updateMerged: function(){
                this.set('merged', this.get('results').fullCollection.length === this.get('mergedResults').fullCollection.length);
            },
            isUnmerged: function(){
                return !this.get('merged');
            },
            mergeNewResults: function(){
                this.lastMerge = Date.now();
                this.set('merged', true);
                this.get('results').set(this.get('mergedResults').fullCollection.models, { remove: false });
                this.get('results').fullCollection.sort();
                this.trigger('sync');
            },
            handleCurrentlyViewed: function(){
                if (!this.get('currentlyViewed') && !this.get('merged')){
                    this.mergeNewResults();
                }
            },
            isSearching: function(){
                return this.get('status').some(function(status){
                    return status.get('successful') === undefined;
                });
            },
            setQueryId: function(queryId){
                this.set('queryId', queryId);
            },
            setColor: function(color){
                this.set('color', color);
            },
            getQueryId: function(){
                return this.get('queryId');
            },
            getColor: function(){
                return this.get('color');
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
            }
        });

        return MetaCard;

    });
