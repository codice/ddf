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
    './property.collection'
], function (Marionette, _, $, CustomElements, PropertyView, PropertyCollection) {

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
         generateSummaryPropertyCollectionView: function(metacards){
             var propertyCollection = new PropertyCollection();
             var propertyArray = [];
             this.summaryWhiteList.forEach(function(property){
                 propertyArray.push({
                     id: property,
                     type: metacards['metacard-types'][0].type[property].type,
                     values: {},
                     multivalued: metacards['metacard-types'][0].type[property].multivalued
                 });
             });
             propertyArray.forEach(function(property){
                 metacards.metacards.forEach(function(metacard){
                     var value = metacard[property.id];
                     if (!metacards['metacard-types'][0].type[property.id].multivalued){
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
                 if (metacards.metacards.length > 1){
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
         generatePropertyCollectionView: function(metacards){
             var propertyCollection = new PropertyCollection();
             var propertyIntersection = this.determinePropertyIntersection(metacards);
             var propertyArray = [];
             propertyIntersection.forEach(function(property){
                 propertyArray.push({
                     id: property,
                     type: metacards['metacard-types'][0].type[property].type,
                     values: {},
                     multivalued: metacards['metacard-types'][0].type[property].multivalued
                 });
             });
             propertyArray.forEach(function(property){
                 metacards.metacards.forEach(function(metacard){
                     var value = metacard[property.id];
                     if (!metacards['metacard-types'][0].type[property.id].multivalued){
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
                 if (metacards.metacards.length > 1){
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
         determinePropertyIntersection: function(metacards){
             var self = this;
             var propertyIntersection = metacards['metacard-types'].map(function(metacardType){
                 return Object.keys(metacardType.type);
             });
             if (propertyIntersection.length === 1){
                 propertyIntersection = propertyIntersection[0];
             } else {
                 propertyIntersection = _.intersection.apply(_, propertyIntersection);
             }
             propertyIntersection = propertyIntersection.filter(function(property){
                 return property === self.thumbnail ||  (property.indexOf('metacard') === -1 &&
                     property.indexOf('.') === -1
                     && property.indexOf('metadata') === -1
                     && property.indexOf('validation') === -1
                     && self.blacklist.indexOf(property) === -1
                     && self.hiddenTypes.indexOf(metacards['metacard-types'][0].type[property].type) === -1
                     && self.bulkHiddenTypes.indexOf(metacards['metacard-types'][0].type[property].type) === -1);
             });
             return propertyIntersection;
         }
     });
});