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
    './value.view',
    './value.collection',
    'js/CustomElements',
    'moment'
], function (Marionette, _, $, ValueView, ValueCollection, CustomElements, moment) {

    return Marionette.CollectionView.extend({
        childView: ValueView,
        tagName: CustomElements.register('value-collection'),
        onRender: function () {
        },
        hasChanged: function(){
            switch(this.model.getCalculatedType()){
                case 'thumbnail':
                case 'location':
                    return this.children.first().hasChanged();
                case 'date':
                    var currentValue = this.children.map(function(childView){
                        return childView.getCurrentValue();
                    });
                    currentValue.sort();
                    return currentValue.toString() !==  this.model.getInitialValue().map(function(dateValue){
                            if (dateValue){
                                return (moment(dateValue)).toISOString();
                            } else {
                                return dateValue;
                            }
                        }).toString();
                case 'number':  //needed until cql result correctly returns numbers as numbers
                    var currentValue = this.children.map(function(childView){
                        return childView.getCurrentValue().toString();
                    });
                    currentValue.sort();
                    return JSON.stringify(currentValue) !== JSON.stringify(this.model.getInitialValue().map(function(value){ 
                        return Number(value).toString(); //handle cases of unnecessary number padding -> 22.0000
                    }));
                default:
                    var currentValue = this.children.map(function(childView){
                        return childView.getCurrentValue();
                    });
                    currentValue.sort();
                    return JSON.stringify(currentValue) !== JSON.stringify(this.model.getInitialValue());
            }
        },
        addNewValue: function (propertyModel){
            this.collection.add({
                value: 'New Value',
                property: propertyModel
            });
            this.children.last().focus();
        },
        getCurrentValue: function(){
            return this.children.map(function(childView){
                return childView.getCurrentValue();
            });
        }
    },{
        generateValueCollectionView: function(propertyModel){
            var valueCollection = new ValueCollection();
            if (propertyModel.get('value').length > 0){
                valueCollection.add(propertyModel.get('value').map(function(value){
                    return {
                        value: value,
                        property: propertyModel
                    }
                }));
            } else if (!propertyModel.get('multivalued')) {
                valueCollection.add({
                        value: null,
                        property: propertyModel
                });
            }
            return new this({
                collection: valueCollection,
                model: propertyModel
            });
        }
    });
});