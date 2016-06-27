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
/*global define, alert*/
define([
    'marionette',
    'underscore',
    'jquery',
    'js/CustomElements',
    './property.view',
    './property.collection',
    'properties',
    'js/store'
], function (Marionette, _, $, CustomElements, PropertyView, PropertyCollection, properties, store) {

     return Marionette.CollectionView.extend({
        tagName: CustomElements.register('property-collection'),
        childView: PropertyView,
        turnOnLimitedWidth: function(){
            this.children.forEach(function(childView){
                childView.turnOnLimitedWidth();
            });
        },
        turnOnEditing: function () {
            this.children.forEach(function (childView) {
                childView.turnOnEditing();
            });
        },
        turnOffEditing: function () {
            this.children.forEach(function (childView) {
                childView.turnOffEditing();
            });
        },
        revert: function () {
            this.children.forEach(function (childView) {
                if (childView.hasChanged()){
                    childView.revert();
                }
            });
        },
        save: function () {
            this.children.forEach(function (childView) {
                childView.save();
            });
        },
        toJSON: function(){
            return this.children.reduce(function (attributeToVal, childView){
                return _.extend(attributeToVal, childView.toJSON());
            }, {});
        },
        toPatchJSON: function(){
            var attributeArray = [];
            this.children.forEach(function (childView){
                var attribute = childView.toPatchJSON();
                if (attribute){
                    attributeArray.push(attribute);
                }
            });
            return attributeArray;
        },
         clearValidation: function(){
             this.children.forEach(function(childView){
                    childView.clearValidation();
             });
         },
        updateValidation: function(validationReport){
            var self = this;
            validationReport.forEach(function(attributeValidationReport){
                self.children.filter(function(childView){
                    return childView.model.get('id') === attributeValidationReport.attribute;
                }).forEach(function(childView){
                    childView.updateValidation(attributeValidationReport);
                });
            });
        },
        focus: function () {
            this.children.first().focus();
        }
    }, {
         //contains methods for generating property collection views from service responses
         bulkHiddenTypes: ['BINARY'],
         hiddenTypes: ['XML', 'OBJECT'],
         blacklist: ['metacard-type', 'source-id', 'cached'],
         thumbnail: 'thumbnail',
         summaryWhiteList: ['created','modified','thumbnail'],
         generateSummaryPropertyCollectionView: function(types, metacards){
             var propertyCollection = new PropertyCollection();
             var propertyArray = [];
             this.summaryWhiteList.forEach(function(property){
                 propertyArray.push({
                     enum: store.enums[property],
                     label: properties.attributeAliases[property],
                     id: property,
                     type: types[0][property].format,
                     values: {},
                     multivalued: types[0][property].multivalued
                 });
             });
             properties.summaryShow.filter(function(property){
                 return types[0][property] !== undefined;
             }).forEach(function(property){
                 propertyArray.push({
                     enum: store.enums[property],
                     label: properties.attributeAliases[property],
                     id: property,
                     type: types[0][property].format,
                     values: {},
                     multivalued: types[0][property].multivalued
                 });
             });
             propertyArray.forEach(function(property){
                 metacards.forEach(function(metacard){
                     var value = metacard[property.id];
                     if (!types[0][property.id].multivalued){
                         value = [value];
                     }
                     value.sort();
                     property.value = value;
                     property.values[value] = property.values[value] || {
                             value: value,
                             hits: 0
                         };
                     property.values[value].hits++;
                 });
                 if (metacards.length > 1){
                     property.bulk = true;
                     if (Object.keys(property.values).length > 1){
                         property.value = [''];
                     }
                 }
             });
             propertyCollection.add(propertyArray);
             return new this({
                 collection: propertyCollection
             });
         },
         generatePropertyCollectionView: function(types, metacards){
             var propertyCollection = new PropertyCollection();
             var propertyIntersection = this.determinePropertyIntersection(types, metacards);
             var propertyArray = [];
             propertyIntersection.forEach(function(property){
                 propertyArray.push({
                     enum: store.enums[property],
                     label: properties.attributeAliases[property],
                     id: property,
                     type: types[0][property].format,
                     values: {},
                     multivalued: types[0][property].multivalued
                 });
             });
             propertyArray.forEach(function(property){
                 metacards.forEach(function(metacard){
                     var value = metacard[property.id];
                     if (!types[0][property.id].multivalued){
                         value = [value];
                     }
                     value.sort();
                     property.value = value;
                     property.values[value] = property.values[value] || {
                             value: value,
                             hits: 0
                         };
                     property.values[value].hits++;
                 });
                 if (metacards.length > 1){
                     property.bulk = true;
                     if (Object.keys(property.values).length > 1){
                         property.value = [''];
                     }
                 }
             });
             propertyCollection.add(propertyArray);
             return new this({
                 collection: propertyCollection
             });
         },
         determinePropertyIntersection: function(types, metacards){
             var self = this;
             var attributeKeys = metacards.map(function(metacard){
                 return Object.keys(metacard);
             });
             var typeKeys = types.map(function(type){
                 return Object.keys(type);
             });
             var propertyIntersection = attributeKeys.concat(typeKeys);
             propertyIntersection = _.intersection.apply(_, propertyIntersection);
             propertyIntersection = propertyIntersection.filter(function(property){
                 return property === self.thumbnail ||  (property.indexOf('metacard') === -1
                     && property.indexOf('metadata') === -1
                     && property.indexOf('validation') === -1
                     && self.blacklist.indexOf(property) === -1
                     && self.hiddenTypes.indexOf(types[0][property].format) === -1
                     && self.bulkHiddenTypes.indexOf(types[0][property].format) === -1);
             });
             return propertyIntersection;
         }
     });
});