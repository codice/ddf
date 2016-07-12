/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define, setTimeout*/
define([
    'marionette',
    'underscore',
    'jquery',
    'text!./query-basic.hbs',
    'js/CustomElements',
    'js/store',
    'component/dropdown/dropdown',
    'component/dropdown/query-src/dropdown.query-src.view',
    'component/property/property.view',
    'component/property/property',
    'js/cql',
    'component/singletons/metacard-definitions',
    'component/singletons/sources-instance'
], function (Marionette, _, $, template, CustomElements, store, DropdownModel,
             QuerySrcView, PropertyView, Property, cql, metacardDefinitions, sources) {

    function sanitizeForCql(text){
        return text.split('[').join('(').split(']').join(')').split("'").join('').split('"').join('');
    }

    function bboxToCQLPolygon(model){
        return [
            model.west + ' ' + model.south,
            model.west + ' ' + model.north,
            model.east + ' ' + model.north,
            model.east + ' ' + model.south,
            model.west + ' ' + model.south
        ];
    }

    function polygonToCQLPolygon(model){
        var cqlPolygon = model.map(function(point){
            return point[0] + ' ' + point[1];
        });
        cqlPolygon.push(cqlPolygon[0]);
        return cqlPolygon;
    }

    function generateAnyGeoFilter(property, model){
        switch(model.type){
            case 'POLYGON':
                return {
                    type: 'INTERSECTS',
                    property: property,
                    value: 'POLYGON('+sanitizeForCql(JSON.stringify(polygonToCQLPolygon(model.polygon)))+')'
                };
                break;
            case 'BBOX':
                return {
                    type: 'INTERSECTS',
                    property: property,
                    value: 'POLYGON('+sanitizeForCql(JSON.stringify(bboxToCQLPolygon(model)))+')'
                };
                break;
            case 'POINTRADIUS':
                return {
                    type: 'DWITHIN',
                    property: property,
                    value: 'POINT(' + model.lon + ' ' + model.lat + ')',
                    distance: Number(model.radius)
                };
                break;
        }
    }

    function generateFilter(type, property, value) {
        switch (metacardDefinitions.metacardTypes[property].type) {
            case 'LOCATION':
            case 'GEOMETRY':
                return generateAnyGeoFilter(property, value);
                break;
            default:
                return {
                    type: type,
                    property: '"' + property + '"',
                    value: value
                };
                break;
        }
    }

    //we should probably regex this or find a better way, but for now this works
    function sanitizeGeometryCql(cqlString){
        return cqlString.split("'POLYGON((").join("POLYGON((").split("))'").join("))")
            .split("'POINT(").join("POINT(").split(")'").join(")");
    }

    function getProperty(filter){
        return filter.property.split('"').join('');
    }

    function isTypeLimiter(filter){
        var typesFound = {};
        filter.filters.forEach(function(subfilter){
            typesFound[getProperty(subfilter)] = true;
        });
        typesFound = Object.keys(typesFound);
        return typesFound.length === 1 && (typesFound[0] === 'metadata-content-type');
    }

    function isAnyDate(filter){
        var propertiesToCheck = ['created','modified','effective'];
        var typesFound = {};
        if (filter.filters.length === 3){
            filter.filters.forEach(function(subfilter){
                typesFound[subfilter.type] = true;
                var indexOfType = propertiesToCheck.indexOf(getProperty(subfilter));
                if (indexOfType >= 0){
                    propertiesToCheck.splice(indexOfType, 1);
                }
            });
            return propertiesToCheck.length === 0 && Object.keys(typesFound).length === 1;
        }
        return false;
    }

    function translateFilterToBasicMap(filter){
        var propertyValueMap = {};
        if (filter.filters){
            filter.filters.forEach(function(filter){
               if (!filter.filters){
                   propertyValueMap[getProperty(filter)] = propertyValueMap[getProperty(filter)] || [];
                   if (propertyValueMap[getProperty(filter)].filter(function(existingFilter){
                           return existingFilter.type === filter.type;
                       }).length === 0) {
                       propertyValueMap[getProperty(filter)].push(filter);
                   }
               } else if (isAnyDate(filter)) {
                   propertyValueMap['anyDate'] = propertyValueMap['anyDate'] || [];
                   if (propertyValueMap['anyDate'].filter(function(existingFilter){
                           return existingFilter.type === filter.filters[0].type;
                       }).length === 0) {
                       propertyValueMap['anyDate'].push(filter.filters[0]);
                   }
               } else if (isTypeLimiter(filter)){
                   propertyValueMap[getProperty(filter.filters[0])] = propertyValueMap[getProperty(filter.filters[0])] || [];
                   filter.filters.forEach(function(subfilter){
                       propertyValueMap[getProperty(filter.filters[0])].push(subfilter);
                   });
               }
            });
        } else {
            propertyValueMap[getProperty(filter)] = propertyValueMap[getProperty(filter)] || [];
            propertyValueMap[getProperty(filter)].push(filter);
        }
        return propertyValueMap;
    }

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('query-basic'),
        modelEvents: {
        },
        events: {
            'click .editor-edit': 'edit',
            'click .editor-cancel': 'cancel',
            'click .editor-save': 'save'
        },
        regions: {
            basicTitle: '.basic-title',
            basicText: '.basic-text',
            basicTextMatch: '.basic-text-match',
            basicTime: '.basic-time',
            basicTimeField: '.basic-time-field',
            basicTimeBefore: '.basic-time-before',
            basicTimeAfter: '.basic-time-after',
            basicTimeBetweenBefore: '.between-before',
            basicTimeBetweenAfter: '.between-after',
            basicLocation: '.basic-location',
            basicLocationSpecific: '.basic-location-specific',
            basicType: '.basic-type',
            basicTypeSpecific: '.basic-type-specific'
        },
        ui: {
        },
        filter: undefined,
        onBeforeShow: function(){
            this.filter = translateFilterToBasicMap(cql.simplify(cql.read(this.model.get('cql'))));
            this.setupTitleInput();
            this.setupTextInput();
            this.setupTextMatchInput();
            this.setupTimeInput();
            this.setupTimeBefore();
            this.setupTimeAfter();
            this.setupTimeBetween();
            this.setupLocation();
            this.setupLocationInput();
            this.setupType();
            this.setupTypeSpecific();
            this.turnOnLimitedWidth();
            this.basicTime.currentView.$el.on('change', this.handleTimeRangeValue.bind(this));
            this.basicLocation.currentView.$el.on('change', this.handleLocationValue.bind(this));
            this.basicType.currentView.$el.on('change', this.handleTypeValue.bind(this));
            this.handleTimeRangeValue();
            this.handleLocationValue();
            this.handleTypeValue();
            if (this.model._cloneOf === undefined){
                this.edit();
            } else {
                this.turnOffEdit();
            }
        },
        setupTypeSpecific: function(){
            var currentValue = [];
            if (this.filter['metadata-content-type']){
                currentValue = this.filter['metadata-content-type'].map(function(subfilter){
                    return subfilter.value;
                });
            }
            this.basicTypeSpecific.show(new PropertyView({
                model: new Property({
                    enumMulti: true,
                    enum: sources.toJSON().reduce(function(enumArray, source){
                        source.contentTypes.forEach(function(contentType){
                            if (contentType.value && (enumArray.filter(function(option){
                                    return option.value === contentType.value;
                                }).length === 0)){
                                enumArray.push({
                                    label: contentType.name,
                                    value: contentType.value
                                });
                            }
                        });
                        return enumArray;
                    }, []),
                    value: [currentValue],
                    id: 'Types'
                })
            }));
        },
        setupType: function(){
            var currentValue = 'any';
            if (this.filter['metadata-content-type']){
                currentValue = 'specific'
            }
            this.basicType.show(new PropertyView({
                model: new Property({
                    value: [currentValue],
                    id: 'Match Types',
                    radio: [{
                        label: 'Any',
                        value: 'any'
                    },{
                        label: 'Specific',
                        value: 'specific'
                    }]
                })
            }));
        },
        setupLocation: function(){
            var currentValue = 'any';
            if (this.filter.anyGeo){
                currentValue = 'specific'
            }
            this.basicLocation.show(new PropertyView({
                model: new Property({
                    value: [currentValue],
                    id: 'Located',
                    radio: [{
                        label: 'Anywhere',
                        value: 'any'
                    },{
                        label: 'Somewhere Specific',
                        value: 'specific'
                    }]
                })
            }));
        },
        setupLocationInput: function(){
            var currentValue = '';
            if (this.filter.anyGeo){
                currentValue = this.filter.anyGeo[0];
            }
            this.basicLocationSpecific.show(new PropertyView({
                model: new Property({
                    value: [currentValue],
                    id: 'Location',
                    type: 'LOCATION'
                })
            }));
        },
        handleTypeValue: function(){
            var type = this.basicType.currentView.getCurrentValue()[0];
            this.$el.toggleClass('is-type-any', type === 'any');
            this.$el.toggleClass('is-type-specific', type === 'specific');
        },
        handleLocationValue: function(){
            var location = this.basicLocation.currentView.getCurrentValue()[0];
            this.$el.toggleClass('is-location-any', location === 'any');
            this.$el.toggleClass('is-location-specific', location === 'specific');
        },
        handleTimeRangeValue: function(){
            var timeRange = this.basicTime.currentView.getCurrentValue()[0];
            this.$el.toggleClass('is-timeRange-any', timeRange === 'any');
            this.$el.toggleClass('is-timeRange-before', timeRange === 'before');
            this.$el.toggleClass('is-timeRange-after', timeRange === 'after');
            this.$el.toggleClass('is-timeRange-between', timeRange === 'between');
        },
        setupTimeBefore: function(){
            var currentBefore = '';
            var currentAfter = '';
            var propertyToCheck;
            if (this.filter.anyDate) {
                propertyToCheck = 'anyDate'
            } else if (this.filter.created) {
                propertyToCheck = 'created';
            } else if (this.filter.modified) {
                propertyToCheck = 'modified'
            } else if (this.filter.effective) {
                propertyToCheck = 'effective';
            }
            if (propertyToCheck) {
                this.filter[propertyToCheck].forEach(function(subfilter){
                    if (subfilter.type === 'BEFORE'){
                        currentBefore = subfilter.value;
                    } else {
                        currentAfter = subfilter.value;
                    }
                });
            }
            this.basicTimeBefore.show(new PropertyView({
                model: new Property({
                    value: [currentBefore],
                    id: 'Before',
                    placeholder: 'Limit search to before this time.',
                    type: 'DATE'
                })
            }));
        },
        setupTimeAfter: function(){
            var currentBefore = '';
            var currentAfter = '';
            var propertyToCheck;
            if (this.filter.anyDate) {
                propertyToCheck = 'anyDate'
            } else if (this.filter.created) {
                propertyToCheck = 'created';
            } else if (this.filter.modified) {
                propertyToCheck = 'modified'
            } else if (this.filter.effective) {
                propertyToCheck = 'effective';
            }
            if (propertyToCheck) {
                this.filter[propertyToCheck].forEach(function(subfilter){
                    if (subfilter.type === 'BEFORE'){
                        currentBefore = subfilter.value;
                    } else {
                        currentAfter = subfilter.value;
                    }
                });
            }
            this.basicTimeAfter.show(new PropertyView({
                model: new Property({
                    value: [currentAfter],
                    id: 'After',
                    placeholder: 'Limit search to after this time.',
                    type: 'DATE'
                })
            }));
        },
        setupTimeBetween: function(){
            var currentBefore = '';
            var currentAfter = '';
            var propertyToCheck;
            if (this.filter.anyDate) {
                propertyToCheck = 'anyDate'
            } else if (this.filter.created) {
                propertyToCheck = 'created';
            } else if (this.filter.modified) {
                propertyToCheck = 'modified'
            } else if (this.filter.effective) {
                propertyToCheck = 'effective';
            }
            if (propertyToCheck) {
                this.filter[propertyToCheck].forEach(function(subfilter){
                    if (subfilter.type === 'BEFORE'){
                        currentBefore = subfilter.value;
                    } else {
                        currentAfter = subfilter.value;
                    }
                });
            }
            this.basicTimeBetweenBefore.show(new PropertyView({
                model: new Property({
                    value: [currentBefore],
                    id: 'Before',
                    placeholder: 'Limit search to before this time.',
                    type: 'DATE'
                })
            }));
            this.basicTimeBetweenAfter.show(new PropertyView({
                model: new Property({
                    value: [currentAfter],
                    id: 'After',
                    placeholder: 'Limit search to after this time.',
                    type: 'DATE'
                })
            }));
        },
        setupTimeInput: function(){
            var currentValue = 'any';
            var propertyToCheck;
            if (this.filter.anyDate) {
                propertyToCheck = 'anyDate'
            } else if (this.filter.created) {
                propertyToCheck = 'created';
            } else if (this.filter.modified) {
                propertyToCheck = 'modified'
            } else if (this.filter.effective) {
                propertyToCheck = 'effective';
            }
            if (propertyToCheck) {
                if (this.filter[propertyToCheck].length > 1) {
                    currentValue = 'between'
                } else if(this.filter[propertyToCheck][0].type === 'AFTER') {
                    currentValue = 'after'
                } else {
                    currentValue = 'before'
                }
            }
            this.basicTime.show(new PropertyView({
                model: new Property({
                    value: [currentValue],
                    id: 'Time Range',
                    radio: [{
                        label: 'Any',
                        value: 'any'
                    },{
                        label: 'Before',
                        value: 'before'
                    },{
                        label: 'After',
                        value: 'after'
                    }, {
                        label: 'Between',
                        value: 'between'
                    }]
                })
            }));
        },
        setupTextMatchInput: function(){
            this.basicTextMatch.show(new PropertyView({
                model: new Property({
                    value: [this.filter.anyText && this.filter.anyText[0].type === 'LIKE' ? 'LIKE' : 'ILIKE'],
                    id: 'Match Case',
                    placeholder: 'Text to search for.  Use "%" or "*" for wildcard.',
                    radio: [{
                        label: 'Yes',
                        value: 'LIKE'
                    },{
                        label: 'No',
                        value: 'ILIKE'
                    }]
                })
            }));
        },
        setupTextInput: function(){
            this.basicText.show(new PropertyView({
                model: new Property({
                    value: [this.filter.anyText ? this.filter.anyText[0].value : ''],
                    id: 'Text',
                    placeholder: 'Text to search for.  Use "%" or "*" for wildcard.'
                })
            }));
        },
        setupTitleInput: function(){
            this.basicTitle.show(new PropertyView({
                model: new Property({
                    value: [this.model.get('title')],
                    id: 'Name',
                    placeholder: 'Name for you to identify the query by.'
                })
            }));
        },
        turnOnLimitedWidth: function(){
            this.regionManager.forEach(function(region){
                if (region.currentView){
                    region.currentView.turnOnLimitedWidth();
                }
            });
        },
        turnOffEdit: function(){
            this.regionManager.forEach(function(region){
                if (region.currentView){
                    region.currentView.turnOffEditing();
                }
            });
        },
        edit: function(){
            this.$el.addClass('is-editing');
            this.regionManager.forEach(function(region){
                if (region.currentView){
                    region.currentView.turnOnEditing();
                }
            });
            this.regionManager.first().currentView.focus();
        },
        cancel: function(){
            if (this.model._cloneOf === undefined){
                store.resetQuery();
            } else {
                this.$el.removeClass('is-editing');
                this.onBeforeShow();
            }
        },
        save: function(){
            this.$el.removeClass('is-editing');
            var title = this.basicTitle.currentView.getCurrentValue()[0];
            title = title === "" ? 'Untitled Query' : title;
            this.model.set({
                title: title
            });

            var filter = this.constructFilter();
            var generatedCQL = sanitizeGeometryCql("(" + cql.write(cql.simplify(cql.read(cql.write(filter)))) + ")");
            this.model.set({
                cql: generatedCQL
            });
            store.saveQuery();
        },
        constructFilter: function(){
            var filters = [];

            var text = this.basicText.currentView.getCurrentValue()[0];
            text = text === "" ? '%' : text;
            var matchCase = this.basicTextMatch.currentView.getCurrentValue()[0];
            filters.push(generateFilter(matchCase, 'anyText', text));

            var timeRange = this.basicTime.currentView.getCurrentValue()[0];
            var timeBefore, timeAfter;
            switch(timeRange){
                case 'before':
                    timeBefore = this.basicTimeBefore.currentView.getCurrentValue()[0];
                    break;
                case 'after':
                    timeAfter = this.basicTimeAfter.currentView.getCurrentValue()[0];
                    break;
                case 'between':
                    timeBefore = this.basicTimeBetweenBefore.currentView.getCurrentValue()[0];
                    timeAfter = this.basicTimeBetweenAfter.currentView.getCurrentValue()[0];
                    break;
            }
            if (timeBefore){
                var timeFilter = {
                    type: 'OR',
                    filters: [
                        generateFilter('BEFORE', 'created', timeBefore),
                        generateFilter('BEFORE', 'modified', timeBefore),
                        generateFilter('BEFORE', 'effective', timeBefore)
                    ]
                };
                filters.push(timeFilter);
            }
            if (timeAfter){
                var timeFilter = {
                    type: 'OR',
                    filters: [
                        generateFilter('AFTER', 'created', timeAfter),
                        generateFilter('AFTER', 'modified', timeAfter),
                        generateFilter('AFTER', 'effective', timeAfter)
                    ]
                };
                filters.push(timeFilter);
            }

            var locationSpecific = this.basicLocation.currentView.getCurrentValue()[0];
            var location = this.basicLocationSpecific.currentView.getCurrentValue()[0];
            var locationFilter = generateFilter(undefined, 'anyGeo', location);
            if (locationSpecific === 'specific' && locationFilter){
                filters.push(locationFilter);
            }

            var types = this.basicType.currentView.getCurrentValue()[0];
            var typesSpecific = this.basicTypeSpecific.currentView.getCurrentValue()[0];
            if (types === 'specific' && typesSpecific.length !== 0){
                var typeFilter = {
                    type: 'OR',
                    filters: typesSpecific.map(function(specificType){
                        return generateFilter('=', 'metadata-content-type', specificType);
                    })
                };
                filters.push(typeFilter)
            }

            return {
                type: 'AND',
                filters: filters
            };
        }
    });
});
