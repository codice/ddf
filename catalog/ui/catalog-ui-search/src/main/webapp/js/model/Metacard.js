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
        '@turf/meta',
        'wkx',
        'moment',
        'properties',
        'component/singletons/user-instance',
        'backboneassociations',
        'backbone.paginator'
    ],
    function (Backbone, _, $, wreqr, metacardDefinitions, Sources, Terraformer, TerraformerWKTParser, CQLUtils,
              Turf, TurfMeta, wkx, moment, properties, user) {
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
                            return new Terraformer.Primitive(wkx.Geometry.parse(metacard.properties[property]).toGeoJSON());
                        });
                        break;
                    default:
                        var valueToCheck = metacard.properties[filter.property.replace(/['"]+/g, '')];
                        if (valueToCheck !== undefined) {
                            valuesToCheck.push(valueToCheck);
                        }
                        break;
                }

                if (valuesToCheck.length === 0) {
                    return filter.value === "";  // aligns with how querying works on the server
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
            var i;
            switch (resultFilter.type) {
                case 'AND':
                    for (i = 0; i <= resultFilter.filters.length - 1; i++) {
                        if (!matchesFilter(metacard, resultFilter.filters[i], metacardTypes)) {
                            return false;
                        }
                    }
                    return true;
                case 'NOT AND':
                    for (i = 0; i <= resultFilter.filters.length - 1; i++) {
                        if (!matchesFilter(metacard, resultFilter.filters[i], metacardTypes)) {
                            return true;
                        }
                    }
                    return false;
                case 'OR':
                    for (i = 0; i <= resultFilter.filters.length - 1; i++) {
                        if (matchesFilter(metacard, resultFilter.filters[i], metacardTypes)) {
                            return true;
                        }
                    }
                    return false;
                case 'NOT OR':
                    for (i = 0; i <= resultFilter.filters.length - 1; i++) {
                        if (matchesFilter(metacard, resultFilter.filters[i], metacardTypes)) {
                            return false;
                        }
                    }
                    return true;
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

        MetaCard.Properties = Backbone.AssociatedModel.extend({
            defaults: function () {
                return {
                    'metacard-tags': ['resource']
                }
            },
            hasGeometry: function (attribute) {
                return _.filter(this.toJSON(), function (value, key) {
                    return (attribute === undefined || attribute === key) && metacardDefinitions.metacardTypes[key] &&
                        metacardDefinitions.metacardTypes[key].type === "GEOMETRY";
                }).length > 0;
            },
            getCombinedGeoJSON: function () {
                return;
            },
            getPoints: function (attribute) {
                return this.getGeometries(attribute).reduce(function (pointArray, wkt) {
                    return pointArray.concat(TurfMeta.coordAll(wkx.Geometry.parse(wkt).toGeoJSON()));
                }, []);
            },
            getGeometries: function (attribute) {
                return _.filter(this.toJSON(), function (value, key) {
                    return !properties.isHidden(key) && (attribute === undefined || attribute === key) && metacardDefinitions.metacardTypes[key] &&
                        metacardDefinitions.metacardTypes[key].type === "GEOMETRY";
                });
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
            hasGeometry: function(attribute){
                return this.get('properties').hasGeometry(attribute);
            },
            getPoints: function(attribute){
                return this.get('properties').getPoints(attribute);
            },
            getGeometries: function(attribute){
                return this.get('properties').getGeometries(attribute);
            },

            relations: [
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
            hasGeometry: function(attribute){
                return this.get('metacard').hasGeometry(attribute);
            },
            getPoints: function(attribute){
                return this.get('metacard').getPoints(attribute);
            },
            getGeometries: function(attribute){
                return this.get('metacard').getGeometries(attribute);
            },
            refreshData: function (){
                //let solr flush
                setTimeout(function (){
                    var metacard = this.get('metacard');
                    var req = {
                        count: 1,
                        cql: CQLUtils.transformFilterToCQL({
                            type: 'AND',
                            filters: [
                                {
                                    type: 'OR',
                                    filters: [
                                        {
                                            type: '=',
                                            property: '"id"',
                                            value: metacard['metacard.deleted.id'] || metacard.id
                                        }, {
                                            type: '=',
                                            property: '"metacard.deleted.id"',
                                            value: metacard.id
                                        }
                                    ]
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
                        src: metacard.get('properties').get('source-id'),
                        start: 1
                    };
                    $.ajax({
                        type: "POST",
                        url: '/search/catalog/internal/cql',
                        data: JSON.stringify(req),
                        dataType: 'json'
                    }).then(this.parseRefresh.bind(this), this.handleRefreshError.bind(this));

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
              pageSize: properties.getPageSize()
            },
            model: MetaCard.MetacardResult,
            mode: "client",
            amountFiltered: 0,
            generateFilteredVersion: function(filter){
                var filteredCollection = new this.constructor();
                filteredCollection.set(this.updateFilteredVersion(filter));
                filteredCollection.amountFiltered = this.amountFiltered;
                return filteredCollection;
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
            defaults: {
                count: 0,
                elapsed: 0,
                hits: 0,
                id: 'undefined',
                successful: undefined,
                top: 0,
                fromcache: 0,
                cacheHasReturned: properties.isCacheDisabled,
                cacheSuccessful: true,
                cacheMessages: [],
                hasReturned: false,
                messages: []
            },
            initialize: function(){
                this.listenToOnce(this, 'change:successful', this.setHasReturned);
            },
            setHasReturned: function(){
                this.set('hasReturned', true);
            },
            setCacheHasReturned: function(){
                this.set('cacheHasReturned', true);
            },
            updateMessages: function(messages, id, status){
                if (this.id === id){
                    this.set('messages', messages);
                }
                if (id === 'cache'){
                    this.set({
                        cacheHasReturned: true,
                        cacheSuccessful: status ? status.successful : false,
                        cacheMessages: messages
                    });
                }
            },
            updateStatus: function(results){
                var top = 0;
                var fromcache = 0;
                results.forEach(function(result){
                    if (result.get('metacard').get('properties').get('source-id') === this.id){
                        top++;
                        if (!result.get('uncached')){
                            fromcache++;
                        }
                    }
                }.bind(this));
                this.set({
                    top: top,
                    fromcache: fromcache
                });
            }
        });

        MetaCard.SearchResult = Backbone.AssociatedModel.extend({
            defaults: {
                'queryId': undefined,
                'results': [],
                'queuedResults': [],
                'merged': true,
                'currentlyViewed': false
            },
            relations: [
                {
                    type: Backbone.Many,
                    key: 'queuedResults',
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
                this.listenTo(this.get('queuedResults'), 'add change remove reset', _.throttle(this.updateMerged, 2500, {leading: false}));
                this.listenTo(this.get('queuedResults'), 'add', _.throttle(this.mergeQueue, 30, {leading: false}));
                this.listenTo(this, 'change:currentlyViewed', this.handleCurrentlyViewed);
                this.listenTo(this, 'error', this.handleError);
                this.listenTo(this, 'sync', this.handleSync);
            },
            handleError: function(resultModel, response, sent){
                var dataJSON = JSON.parse(sent.data);
                this.updateMessages(response.responseJSON ? response.responseJSON.message : response.statusText, dataJSON.src);
            },
            handleSync: function(resultModel, response, sent){
                this.updateStatus();
                if (sent) {
                    var dataJSON = JSON.parse(sent.data);
                    this.updateMessages(response.status.messages, dataJSON.src, response.status);
                }
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
                        if (resp.status.id !== 'cache'){
                            result.uncached = true;
                        }
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
                    queuedResults: resp.results,
                    results: [],
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
            mergeQueue: function(userTriggered){
                if (userTriggered === true || this.allowAutoMerge()) {
                    this.lastMerge = Date.now();
                    this.set('merged', true);
                    var interimCollection = new MetaCard.Results(this.get('results').fullCollection.models);
                    interimCollection.add(this.get('queuedResults').fullCollection.models, {merge: true});
                    interimCollection.fullCollection.comparator = this.get('results').fullCollection.comparator;
                    interimCollection.fullCollection.sort();
                    this.get('results').fullCollection.reset(interimCollection.fullCollection.slice(0, user.get('user').get('preferences').get('resultCount')));
                    this.get('queuedResults').fullCollection.reset();
                    this.updateStatus();
                }
            },
            cacheHasReturned: function(){
                return this.get('status').filter(function(statusModel){
                    return statusModel.id === 'cache';
                }).reduce(function(hasReturned, statusModel){
                    return statusModel.get('successful') !== undefined;
                }, false);
            },
            setCacheChecked: function(){
                if (this.cacheHasReturned()) {
                    this.get('status').forEach(function(statusModel){
                        statusModel.setCacheHasReturned();
                    }.bind(this));
                }
            },
            updateMessages: function(message, id, status){
                this.get('status').forEach(function(statusModel){
                    statusModel.updateMessages(message, id, status);
                }.bind(this));
            },
            updateStatus: function(){
                this.setCacheChecked();
                this.get('status').forEach(function(statusModel){
                    statusModel.updateStatus(this.get('results').fullCollection);
                }.bind(this));
            },
            updateMerged: function(){
                this.set('merged', this.get('queuedResults').fullCollection.length === 0);
            },
            isUnmerged: function(){
                return !this.get('merged');
            },
            mergeNewResults: function(){
                this.mergeQueue(true);
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
