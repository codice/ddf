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
    '../input.view',
    '../thumbnail/input-thumbnail.view',
    '../date/input-date.view',
    '../bulk/input-bulk.view'
], function (Marionette, _, $, CustomElements, InputView, InputThumbnailView, InputDateView, InputBulkView) {

    var InputCollectionView = Marionette.CollectionView.extend({
        tagName: CustomElements.register('input-metacard-collection'),
        getChildView: function (item) {
            switch (item.type) {
                case 'date':
                    return InputDateView;
                case 'thumbnail':
                    return InputThumbnailView;
                case 'text':
                    return InputView;
                case 'bulk':
                    return InputBulkView;
            }
        },
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
                childView.revert();
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
    });

    return InputCollectionView;
});