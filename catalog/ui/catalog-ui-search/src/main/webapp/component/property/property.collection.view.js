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
    'component/singletons/metacard-definitions',
    'component/announcement'
], function(Marionette, _, $, CustomElements, PropertyView, PropertyCollection, properties, metacardDefinitions,
        announcement) {

    return Marionette.CollectionView.extend({
        tagName: CustomElements.register('property-collection'),
        childView: PropertyView,
        turnOnLimitedWidth: function() {
            this.children.forEach(function(childView) {
                childView.turnOnLimitedWidth();
            });
        },
        turnOnEditing: function() {
            this.children.forEach(function(childView) {
                childView.turnOnEditing();
            });
        },
        turnOffEditing: function() {
            this.children.forEach(function(childView) {
                childView.turnOffEditing();
            });
        },
        revert: function() {
            this.children.forEach(function(childView) {
                if (childView.hasChanged()) {
                    childView.revert();
                }
            });
        },
        save: function() {
            this.children.forEach(function(childView) {
                childView.save();
            });
        },
        toJSON: function() {
            return this.children.reduce(function(attributeToVal, childView) {
                return _.extend(attributeToVal, childView.toJSON());
            }, {});
        },
        toPatchJSON: function() {
            var attributeArray = [];
            this.children.forEach(function(childView) {
                var attribute = childView.toPatchJSON();
                if (attribute) {
                    attributeArray.push(attribute);
                }
            });
            return attributeArray;
        },
        clearValidation: function() {
            this.children.forEach(function(childView) {
                childView.clearValidation();
            });
        },
        updateValidation: function(validationReport) {
            var self = this;
            validationReport.forEach(function(attributeValidationReport) {
                self.children.filter(function(childView) {
                    return childView.model.get('id') === attributeValidationReport.attribute;
                }).forEach(function(childView) {
                    childView.updateValidation(attributeValidationReport);
                });
            });
        },
        focus: function() {
            this.children.first().focus();
        }
    }, {
        //contains methods for generating property collection views from service responses
        summaryWhiteList: ['created', 'modified', 'thumbnail'],
        generateSummaryPropertyCollectionView: function(metacards) {
            var propertyArray = [];
            this.summaryWhiteList.forEach(function(property) {
                if (Boolean(metacardDefinitions.metacardTypes[property])) {
                    propertyArray.push({
                        enumFiltering: true,
                        enum: metacardDefinitions.enums[property],
                        label: properties.attributeAliases[property],
                        readOnly: properties.isReadOnly(property),
                        id: property,
                        type: metacardDefinitions.metacardTypes[property].type,
                        values: {},
                        multivalued: metacardDefinitions.metacardTypes[property].multivalued
                    });
                } else {
                    announcement.announce({
                        title: 'Missing Attribute Definition',
                        message: 'Could not find information for '+property+' in definitions.  If this problem persists, contact your Administrator.',
                        type: 'warn'
                    });
                }
            });
            properties.summaryShow.forEach(function(property) {
                if (Boolean(metacardDefinitions.metacardTypes[property])){
                    propertyArray.push({
                        enumFiltering: true,
                        enum: metacardDefinitions.enums[property],
                        label: properties.attributeAliases[property],
                        readOnly: properties.isReadOnly(property),
                        id: property,
                        type: metacardDefinitions.metacardTypes[property].type,
                        values: {},
                        multivalued: metacardDefinitions.metacardTypes[property].multivalued
                    });
                } else {
                    announcement.announce({
                        title: 'Missing Attribute Definition',
                        message: 'Could not find information for '+property+' in definitions.  If this problem persists, contact your Administrator.',
                        type: 'warn'
                    });
                }
            });
            return this.generateCollectionView(propertyArray, metacards);
        },
        generatePropertyCollectionView: function(metacards) {
            var propertyCollection = new PropertyCollection();
            var propertyIntersection = this.determinePropertyIntersection(metacards);
            var propertyArray = [];
            propertyIntersection.forEach(function(property) {
                propertyArray.push({
                    enumFiltering: true,
                    enum: metacardDefinitions.enums[property],
                    label: properties.attributeAliases[property],
                    readOnly: properties.isReadOnly(property),
                    id: property,
                    type: metacardDefinitions.metacardTypes[property].type,
                    values: {},
                    multivalued: metacardDefinitions.metacardTypes[property].multivalued
                });
            });
            return this.generateCollectionView(propertyArray, metacards);
        },
        generateCollectionView: function(propertyArray, metacards){
            propertyArray.forEach(function(property) {
                metacards.forEach(function(metacard) {
                    var value = metacard[property.id];
                    if (value !== undefined) {
                        if (!metacardDefinitions.metacardTypes[property.id].multivalued){
                            if (value.sort === undefined){
                                value = [value];
                            } else {
                                announcement.announce({
                                    title: 'Conflicting Attribute Definition',
                                    message: property.id+' claims to be singlevalued by definition, but the value on the result is not.  If this problem persists, contact your Administrator.',
                                    type: 'warn'
                                });
                            }
                        } else if (value.sort === undefined){
                            announcement.announce({
                                title: 'Conflicting Attribute Definition',
                                message: property.id+' claims to be multivalued by definition, but the value on the result is not.  If this problem persists, contact your Administrator.',
                                type: 'warn'
                            });
                            value = [value];
                        }
                    } else {
                        value = [value];
                    }
                    value.sort();
                    property.value = value;
                    property.values[value] = property.values[value] || {
                        value: value,
                        hits: 0,
                        ids: []
                    };
                    property.values[value].ids.push(metacard.id);
                    property.values[value].hits++;
                });
                if (metacards.length > 1) {
                    property.bulk = true;
                    if (Object.keys(property.values).length > 1) {
                        property.value = [''];
                    }
                }
            });
            return new this({
                collection: new PropertyCollection(propertyArray)
            });
        },
        determinePropertyIntersection: function(metacards) {
            var self = this;
            var attributeKeys = metacards.map(function(metacard) {
                return Object.keys(metacard);
            });
            var propertyIntersection = _.intersection.apply(_, attributeKeys);
            propertyIntersection = propertyIntersection.filter(function(property) {
                if (metacardDefinitions.metacardTypes[property]){
                    return (!properties.isHidden(property)
                    && !metacardDefinitions.isHiddenType(property));
                } else {
                    announcement.announce({
                        title: 'Missing Attribute Definition',
                        message: 'Could not find information for '+property+' in definitions.  If this problem persists, contact your Administrator.',
                        type: 'warn'
                    });
                    return false;
                }
            }).sort();
            return propertyIntersection;
        }
    });
});
