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
    'properties'
], function ($, Backbone, _, properties) {

    return new (Backbone.Model.extend({
        initialize: function () {
            this.getMetacardTypes();
        },
        getEnums: function(){
            $.when.apply(this, this.metacardDefinitions.map(function(metacardDefinition){
                return $.get( '/search/catalog/internal/enumerations/'+metacardDefinition);
            })).always(function(){
                _.forEach(arguments, function(response){
                    _.extend(this.enums, response[0]);
                }.bind(this));
            }.bind(this));
        },
        getMetacardTypes: function(){
            $.get('/search/catalog/internal/metacardtype').then(function(metacardTypes){
                for (var metacardType in metacardTypes){
                    if (metacardTypes.hasOwnProperty(metacardType)) {
                        this.metacardDefinitions.push(metacardType);
                        for (var type in metacardTypes[metacardType]) {
                            if (metacardTypes[metacardType].hasOwnProperty(type)) {
                                this.metacardTypes[type] = metacardTypes[metacardType][type];
                                this.metacardTypes[type].alias = properties.attributeAliases[type];
                            }
                        }
                    }
                }
                for (var propertyType in this.metacardTypes){
                    if (this.metacardTypes.hasOwnProperty(propertyType)) {
                        this.sortedMetacardTypes.push(this.metacardTypes[propertyType]);
                    }
                }
                this.sortedMetacardTypes.sort(function(a, b){
                    var attrToCompareA = a.alias || a.id;
                    var attrToCompareB = b.alias || b.id;
                    if (attrToCompareA < attrToCompareB){
                        return -1;
                    }
                    if (attrToCompareA > attrToCompareB){
                        return 1;
                    }
                    return 0;
                });
                this.getEnums();
            }.bind(this));
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
            }
        },
        enums: {
        }
    }))();
});
