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
define([
    'backbone',
    'underscore',
    'moment',
    'jquery',
    'properties',
    './CQL.factory'
], function(Backbone, _, moment,$, Properties, CQLFactory) {
    "use strict";


    var excludeFromCQL = [Properties.filters.SOURCE_ID, Properties.filters.ANY_GEO];

    var Filter = {};

    Filter.Model = Backbone.Model.extend({
        defaults: {
            fieldName: null,
            fieldType: null,
            fieldOperator: null,
            stringValue1: '',
            dateValue1: '',
            numberValue1: '',
            geoType: 'bbox'  // default to bbox.
        },
        toCQL: function(){
            return CQLFactory.toCQL(this);
        },

        isIgnoredFromCQL: function(){
          return _.contains(excludeFromCQL, this.get('fieldName'));
        },

        isValidGeo: function(){
            var north = this.get('north'),
                south = this.get('south'),
                west = this.get('west'),
                east = this.get('east'),
                lat = this.get('lat'),
                lon = this.get('lon'),
                radius = this.get('radius'),
                geoType = this.get('geoType'),
                polygon = this.get('polygon');
            if (north && south && east && west && geoType === 'bbox') {
                return true;
            } else if (lat && lon && radius && geoType === 'circle') {
                return true;
            } else if(polygon && geoType === 'polygon'){
                return true;
            }
            return false;
        }
    });

    Filter.Collection = Backbone.Collection.extend({
        model: Filter.Model,
        toCQL: function(){
            var cqlArray = this.map(function(model){
                if(!model.isIgnoredFromCQL()){
                    return model.toCQL();
                }
            });

            // anyGeo will be auto grouped together using the OR operator.
            // this is a simple way to support multiple geo filtering.
            var anyGeos = this.where({fieldName: 'anyGeo'});
            var anyGeoCql = [];
            _.each(anyGeos, function(anyGeo){
                anyGeoCql.push(' ( ' + anyGeo.toCQL() + ' ) ');
            });
            if(!_.isEmpty(anyGeoCql)){
                cqlArray.push(' ( ' + anyGeoCql.join(' OR ') + ' ) ');
            }

            cqlArray = _.compact(cqlArray);
            return cqlArray.join(' AND ');
        },
        trimUnfinishedFilters: function(){
            var unfinished = this.filter(function(filter){

                // sorry this is messy.  I basically want a way to trim all filters that don't have values.
                var type = filter.get('fieldType');
                var stringValue1 = $.trim(filter.get('stringValue1'));
                var dateValue1 = $.trim(filter.get('dateValue1'));
                var numberValue1 = $.trim(filter.get('numberValue1'));
                var geoType = filter.get('geoType');
                var hasString = (type === 'string' || type === 'xml') && stringValue1 && stringValue1 !== '';
                var hasNumber = type === 'number' && numberValue1 && numberValue1 !== '';
                var hasDate = type === 'date' && dateValue1 && dateValue1 !== '';
                var hasGeo = type === 'anyGeo' && geoType && filter.isValidGeo();
                if(hasNumber || hasString || hasDate || hasGeo){
                    return false; // no value value.
                }
                return true;
            });
            this.remove(unfinished);
        },

        getGroupedFilterValues: function(fieldName){
            var groupedFilterValues = [];
            var existingFilters = this.where({fieldName: fieldName});
            _.each(existingFilters, function(existingFilter){
                var existingFilterString = existingFilter.get('stringValue1');  // we will assume string for group filters
                if(existingFilterString && existingFilterString !== ''){
                    var parsedIds = existingFilterString.split(',');
                    _.each(parsedIds, function(parsedId){
                        groupedFilterValues.push(parsedId);
                    });
                }
            });
            return _.uniq(groupedFilterValues);
        },

        addValueToGroupFilter: function(fieldName, value){
            var existingFilters = this.where({fieldName: fieldName});
            var groupedFilterValues = [];
            _.each(existingFilters, function(existingFilter){
                var existingFilterString = existingFilter.get('stringValue1');  // we assume string group filters.
                if(existingFilterString && existingFilterString !== ''){
                    var parsedIds = existingFilterString.split(',');
                    _.each(parsedIds, function(parsedId){
                        if(parsedId !== value){
                            groupedFilterValues.push(parsedId);
                        }
                    });
                }
            });
            groupedFilterValues.push(value);
            this.remove(existingFilters);
            this.add(new Filter.Model({
                fieldName: fieldName,
                fieldType: 'string',
                fieldOperator: 'contains',
                stringValue1: groupedFilterValues.join(',')
            }));
        },

        removeAllFromGroupFilter: function(fieldName){
            var existingFilters = this.where({fieldName: fieldName});
            this.remove(existingFilters);
        },

        removeValueFromGroupFilter: function(fieldName, value, defaultValue){
            var existingFilters = this.where({fieldName: fieldName});
            var groupedFilterValues = [];
            _.each(existingFilters, function(existingFilter){
                var existingFilterString = existingFilter.get('stringValue1'); // we assume string group filters.
                if(existingFilterString && existingFilterString !== ''){
                    var parsedIds = existingFilterString.split(',');
                    _.each(parsedIds, function(parsedId){
                        if(parsedId !== value){
                            groupedFilterValues.push(parsedId);
                        }
                    });
                }
            });
            this.remove(existingFilters);
            var fieldValue = groupedFilterValues.join(',');
            if (fieldValue === '' && defaultValue) {
                fieldValue = defaultValue;
            }
            this.add(new Filter.Model({
                fieldName: fieldName,
                fieldType: 'string',
                fieldOperator: 'contains',
                stringValue1: fieldValue
            }));
        },

        replaceGroupFilter: function(fieldName, value){
            var existingFilters = this.where({fieldName: fieldName});
            this.remove(existingFilters);
            this.add(new Filter.Model({
                fieldName: fieldName,
                fieldType: 'string',
                fieldOperator: 'contains',
                stringValue1: value
            }));
        }
    });

    return Filter;
});