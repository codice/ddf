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
/*global define*/

define([
    'jquery',
    'backbone',
    'underscore',
    'properties',
    'moment'
], function ($, Backbone, _, properties, moment) {

    function transformEnumResponse(metacardTypes, response) {
        return _.reduce(response, function (result, value, key) {
            switch (metacardTypes[key].type) {
                case 'DATE':
                    result[key] = value.map(function (subval) {
                        if (subval) {
                            return (moment(subval)).toISOString();
                        }
                        return subval;
                    });
                    break;
                case 'LONG':
                case 'DOUBLE':
                case 'FLOAT':
                case 'INTEGER':
                case 'SHORT': //needed until enum response correctly returns numbers as numbers
                    result[key] = value.map(function (subval) {
                        return Number(subval); //handle cases of unnecessary number padding -> 22.0000
                    });
                    break;
                default:
                    result[key] = value;
                    break;
            }
            return result;
        }, {});
    }

    return new (Backbone.Model.extend({
        initialize: function () {
            this.updateSortedMetacardTypes();
            this.getMetacardTypes();
            this.getDatatypeEnum();
        },
        isHiddenTypeExceptThumbnail: function(id){
            if (id === 'thumbnail'){
                return false;
            } else {
                return this.isHiddenType(id);
            }
        },
        isHiddenType: function(id){
            return this.metacardTypes[id].type === 'XML' ||
            this.metacardTypes[id].type === 'BINARY' ||
            this.metacardTypes[id].type === 'OBJECT';
        },
        getDatatypeEnum: function(){
            $.get( '/intrigue/internal/enumerations/attribute/datatype').then(function(response){
                _.extend(this.enums, response);
            }.bind(this));
        },
        getEnumForMetacardDefinition: function(metacardDefinition){
            $.get( '/intrigue/internal/enumerations/metacardtype/'+metacardDefinition).then(function(response){
                _.extend(this.enums, transformEnumResponse(this.metacardTypes, response));
            }.bind(this));
        },
        addMetacardDefinition: function(metacardDefinitionName, metacardDefinition){
            if (this.metacardDefinitions.indexOf(metacardDefinitionName) === -1){
                this.getEnumForMetacardDefinition(metacardDefinitionName);
                this.metacardDefinitions.push(metacardDefinitionName);
                for (var type in metacardDefinition) {
                    if (metacardDefinition.hasOwnProperty(type)) {
                        this.metacardTypes[type] = metacardDefinition[type];
                        this.metacardTypes[type].id = this.metacardTypes[type].id || type;
                        this.metacardTypes[type].type = this.metacardTypes[type].type || this.metacardTypes[type].format;
                        this.metacardTypes[type].alias = properties.attributeAliases[type];
                    }
                }
                return true;
            }
            return false;
        },
        addMetacardDefinitions: function(metacardDefinitions){
            var updated = false;
            for (var metacardDefinition in metacardDefinitions){
                if (metacardDefinitions.hasOwnProperty(metacardDefinition)) {
                    updated = this.addMetacardDefinition(metacardDefinition, metacardDefinitions[metacardDefinition]) || updated;
                }
            }
            if (updated){
                this.updateSortedMetacardTypes();
            }
        },
        getMetacardTypes: function(){
            $.get('/intrigue/internal/metacardtype').then(function(metacardDefinitions){
                this.addMetacardDefinitions(metacardDefinitions);
            }.bind(this));
        },
        updateSortedMetacardTypes: function(){
            this.sortedMetacardTypes = [];
            for (var propertyType in this.metacardTypes){
                if (this.metacardTypes.hasOwnProperty(propertyType)) {
                    this.sortedMetacardTypes.push(this.metacardTypes[propertyType]);
                }
            }
            this.sortedMetacardTypes.sort(function(a, b){
                var attrToCompareA = (a.alias || a.id).toLowerCase();
                var attrToCompareB = (b.alias || b.id).toLowerCase();
                if (attrToCompareA < attrToCompareB){
                    return -1;
                }
                if (attrToCompareA > attrToCompareB){
                    return 1;
                }
                return 0;
            });
        },
        getLabel: function(id){
            var definition = this.metacardTypes[id];
            return definition ? definition.alias || id : id;
        },
        metacardDefinitions: [],
        sortedMetacardTypes: [],
        metacardTypes: {
            anyText: {
                id: 'anyText',
                type: 'STRING',
                multivalued: false
            },
            anyGeo: {
                id: 'anyGeo',
                type: 'LOCATION',
                multivalued: false
            },
            'metacard-type': {
                id: 'metacard-type',
                type: 'STRING',
                multivalued: false
            },
            'source-id': {
                id: 'source-id',
                type: 'STRING',
                multivalued: false
            },
            cached: {
                id: 'cached',
                type: 'STRING',
                multivalued: false
            },
            'metacard-tags': {
                id: 'metacard-tags',
                type: 'STRING',
                multivalued: true
            }
        },
        validation: {
        },
        enums: {
        }
    }))();
});
