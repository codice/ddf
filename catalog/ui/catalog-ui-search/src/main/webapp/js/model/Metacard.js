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
/*global define, Terraformer*/

define([
        'backbone',
        'underscore',
        'wreqr',
        'terraformerWKTParser',
        'backboneassociations',
        'backbonepaginator'
    ],
    function (Backbone, _, wreqr) {
        "use strict";

        var blacklist = ['metacard-type', 'source-id', 'cached', 'metacard-tags'];

        function matchesILIKE(value, filter){
            var valueToCheckFor = filter.value.toLowerCase();
            value = value.toLowerCase();
            var tokens = value.split(' ');
            for (var i = 0; i <= tokens.length - 1; i++){
                if (tokens[i] === valueToCheckFor){
                    return true;
                }
            }
            return false;
        }

        function matchesLIKE(value, filter){
            var valueToCheckFor = filter.value;
            var tokens = value.split(' ');
            for (var i = 0; i <= tokens.length - 1; i++){
                if (tokens[i] === valueToCheckFor){
                    return true;
                }
            }
            return false;
        }

        function matchesEQUALS(value, filter) {
            var valueToCheckFor = filter.value;
            if (value === valueToCheckFor) {
                return true;
            }
            return false;
        }

        function matchesPOLYGON(value, filter){
            var polygonToCheck = Terraformer.WKT.parse(filter.value.value);
            if (polygonToCheck.contains(value)){
                return true;
            }
            return false;
        }

        function matchesCIRCLE(value, filter){
            var points = filter.value.value.substring(6, filter.value.value.length-1).split(' ');
            var circleToCheck = new Terraformer.Circle(points, filter.distance, 64);
            var polygonCircleToCheck = new Terraformer.Polygon(circleToCheck.geometry);
            if (polygonCircleToCheck.contains(value)){
                return true;
            }
            return false;
        }

        function matchesBEFORE(value, filter){
            var date1 = new Date(value);
            var date2 = new Date(filter.value);
            if (date1 <= date2){
                return true;
            }
            return false;
        }

        function matchesAFTER(value, filter){
            var date1 = new Date(value);
            var date2 = new Date(filter.value);
            if (date1 >= date2){
                return true;
            }
            return false;
        }

        function matchesFilter(metacard, filter, metacardTypes) {
            var valuesToCheck = [];
            switch(filter.property){
                case '"anyText"':
                    valuesToCheck = Object.keys(metacard.properties).filter(function(property){
                        return blacklist.indexOf(property) === -1;
                    }).filter(function(property){
                        return (metacardTypes[property].type === 'STRING');
                    }).map(function(property){
                        return metacard.properties[property];
                    });
                    break;
                case 'anyGeo':
                    valuesToCheck = Object.keys(metacard.properties).filter(function(property){
                        return blacklist.indexOf(property) === -1;
                    }).filter(function(property){
                        return (metacardTypes[property].type === 'GEOMETRY');
                    }).map(function(property){
                        return new Terraformer.Primitive(metacard.properties[property]);
                    });
                    if (metacard.geometry){
                        valuesToCheck.push(new Terraformer.Primitive(metacard.geometry));
                    }
                    break;
                default:
                    var valueToCheck = metacard.properties[filter.property.replace(/['"]+/g, '')];
                    if (valueToCheck) {
                        valuesToCheck.push(valueToCheck);
                    }
                    break;
            }

            if (valuesToCheck.length === 0){
                return false;
            }

            for (var i = 0; i <= valuesToCheck.length - 1 ; i++) {
                switch (filter.type) {
                    case 'ILIKE':
                        if (matchesILIKE(valuesToCheck[i], filter)) {
                            return true;
                        }
                        break;
                    case 'LIKE':
                        if (matchesLIKE(valuesToCheck[i], filter)){
                            return true;
                        }
                        break;
                    case '=':
                        if (matchesEQUALS(valuesToCheck[i], filter)){
                            return true;
                        }
                        break;
                    case 'INTERSECTS':
                        if(matchesPOLYGON(valuesToCheck[i], filter)){
                            return true;
                        }
                        break;
                    case 'DWITHIN':
                        if (matchesCIRCLE(valuesToCheck[i], filter)){
                            return true;
                        }
                        break;
                    case 'AFTER':
                        if (matchesAFTER(valuesToCheck[i], filter)){
                            return true;
                        }
                        break;
                    case 'BEFORE':
                        if (matchesBEFORE(valuesToCheck[i], filter)){
                            return true;
                        }
                        break;
                }
            }
            return false;
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

        function checkSortValue(a, b, sorting){
            var sortOrder = sorting.direction === 'descending' ? -1 : 1;
            var aVal = a.get('metacard>properties>' + sorting.attribute);
            var bVal = b.get('metacard>properties>' + sorting.attribute);
            if (aVal && aVal.constructor === Array){
                aVal = aVal[0];
            }
            if (bVal && bVal.constructor === Array){
                bVal = bVal[0];
            }
            if (aVal < bVal) {
                return sortOrder * -1;
            }
            if (aVal > bVal) {
                return sortOrder;
            }
            return 0;
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
                    }));
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
            defaults: {
            },
            relations: [
                {
                    type: Backbone.One,
                    key: 'metacard',
                    relatedModel: MetaCard.Metacard
                }
            ],
            initialize: function(){
            }
        });

        MetaCard.Results = Backbone.PageableCollection.extend({
            model: MetaCard.MetacardResult,
            mode: "client",
            generateFilteredVersion: function(filter, metacardTypes){
                var filteredCollection = new this.constructor();
                filteredCollection.set(this.updateFilteredVersion(filter, metacardTypes));
                filteredCollection.listenToOriginalCollection(this, filter, metacardTypes);
                return filteredCollection;
            },
            listenToOriginalCollection: function(originalCollection, filter, metacardTypes){
                var debouncedUpdate = _.debounce(function(){
                    this.reset(originalCollection.updateFilteredVersion(filter, metacardTypes));
                }.bind(this), 200);
                this.listenTo(originalCollection, 'add', debouncedUpdate);
                this.listenTo(originalCollection, 'remove',debouncedUpdate);
                this.listenTo(originalCollection, 'update', debouncedUpdate);
            },
            updateFilteredVersion: function(filter, metacardTypes){
                if (filter ) {
                    return this.fullCollection.filter(function (result) {
                        return matchesFilters(result.get('metacard').toJSON(), filter, metacardTypes);
                    });
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
            }
        });

        MetaCard.SourceStatus = Backbone.AssociatedModel.extend({

        });

        MetaCard.SearchResult = Backbone.AssociatedModel.extend({
            defaults: {
                'queryId': undefined,
                'results': []
            },
            relations: [
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
            },
            parse: function (resp) {
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
                            result.metacard.properties.thumbnail = thumbnailAction.url;
                        }
                    });
                }

                return resp;
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
            },
            mergeLatest: function () {
                console.log(this.lastResponse);
                var queryId = this.getQueryId();
                var color = this.getColor();
                if (this.lastResponse) {
                    var update = this.parse(this.lastResponse);
                    if (update && this.get('results') && this.get('results').length > 0) {
                        var selectedForSave = this.get('results').filter(function (result) {
                            return result.get('metacard').has('selectedForSave') &&
                                result.get('metacard').get('selectedForSave') === true;
                        });
                        _.forEach(update.results, function (result) {
                            result.propertyTypes = update['metacard-types'][result.metacard.properties['metacard-type']];
                            result.metacardType = result.metacard.properties['metacard-type'];
                            result.metacard.id = result.metacard.properties.id;
                            result.id = result.metacard.id;
                            result.metacard.queryId = queryId;
                            result.metacard.color = color;
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