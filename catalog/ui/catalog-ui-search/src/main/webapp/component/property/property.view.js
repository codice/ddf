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
    './property.hbs',
    'js/CustomElements',
    'component/input/bulk/input-bulk.view',
    'component/multivalue/multivalue.view'
], function (Marionette, _, $, template, CustomElements, BulkInputView, MultivalueView) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('property'),
        attributes: function(){
            return {
                'data-id': this.model.get('id')
            }
        },
        events: {
            'click .property-revert': 'revert',
            'keyup input': 'handleRevert',
            'click': 'handleRevert',
            'dp.change': 'handleRevert',
            'change': 'handleRevert'
        },
        modelEvents: {
            'change:isEditing': 'handleEdit'
        },
        regions: {
            propertyValue: '.property-value'
        },
        serializeData: function () {
            return _.extend(this.model.toJSON(), {cid: this.cid});
        },
        onRender: function () {
            this.handleEdit();
            this.handleReadOnly();
            this.handleValue();
            this.handleRevert();
            this.handleValidation();
        },
        onBeforeShow: function() {
            this.propertyValue.show(new BulkInputView({
                model: this.model
            }));
        },
        handleReadOnly: function () {
            this.$el.toggleClass('is-readOnly', this.model.isReadOnly());
        },
        handleEdit: function () {
            this.$el.toggleClass('is-editing', this.model.get('isEditing'));
        },
        handleValue: function(){
            this.$el.find('input').val(this.model.getValue());
        },
        turnOnEditing: function(){
            this.model.set('isEditing', true);
        },
        turnOffEditing: function(){
            this.model.set('isEditing', false);
        },
        turnOnLimitedWidth: function(){
            this.$el.addClass('has-limited-width');
        },
        revert: function(){
            this.onBeforeShow();
            this.$el.trigger('change');
        },
        save: function(){
            var value = this.$el.find('input').val();
            this.model.save(value);
        },
        toJSON: function(){
            var attributeToVal = {};
            attributeToVal[this.model.getId()] = this.model.getValue();
            return attributeToVal;
        },
        toPatchJSON: function(){
            if (this.hasChanged()){
                return {
                    attribute: this.model.getId(),
                    values: this.model.getCalculatedType() !== 'date' ?
                        this.getCurrentValue() :
                        this.getCurrentValue().map(function(test){
                            return (new Date(test)).toISOString();
                        })
                };
            } else {
                return undefined;
            }
        },
        getCurrentValue: function(){
            return this.propertyValue.currentView.getCurrentValue();
        },
        focus: function(){
            setTimeout(function() {
                this.$el.find('input').select()
            }.bind(this), 0);
        },
        hasChanged: function(){
            if (this.propertyValue.currentView){
                return this.propertyValue.currentView.hasChanged();
            } else {
                return false;
            }
        },
        handleRevert: function(){
            if (this.hasChanged()){
                this.$el.addClass('is-changed');
            } else {
                this.$el.removeClass('is-changed');
            }
        },
        updateValidation: function(validationReport){
            this._validationReport = validationReport;
            var $validationElement = this.$el.find('> .property-label .property-validation');
            if (validationReport.errors.length > 0){
                this.$el.removeClass('has-warning').addClass('has-error');
                $validationElement.removeClass('is-hidden').removeClass('is-warning').addClass('is-error');
                var validationMessage = validationReport.errors.reduce(function(totalMessage, currentMessage){
                    return totalMessage + currentMessage;
                }, '');
                $validationElement.attr('title', validationMessage);
            } else if (validationReport.warnings.length > 0) {
                this.$el.addClass('has-warning').removeClass('has-error');
                $validationElement.removeClass('is-hidden').removeClass('is-error').addClass('is-warning');
                var validationMessage = validationReport.warnings.reduce(function(totalMessage, currentMessage){
                    return totalMessage + currentMessage;
                }, '');
                $validationElement.attr('title', validationMessage);
            }
            this.handleBulkValidation(validationReport);
        },
        handleBulkValidation: function(validationReport){
            var elementsToCheck = this.$el.find('.is-bulk > .if-viewing .list-value');
            _.forEach(elementsToCheck, function(element){
                 if ($(element).attr('data-ids').split(',').indexOf(validationReport.id) !== -1){
                     var $validationElement = $(element).find('.cell-validation');
                     if (validationReport.errors.length > 0){
                         $validationElement.removeClass('is-hidden').removeClass('is-warning').addClass('is-error');
                         var validationMessage = validationReport.errors.reduce(function(totalMessage, currentMessage){
                             return totalMessage + currentMessage;
                         }, '');
                         $validationElement.attr('title', validationMessage);
                     } else if (validationReport.warnings.length > 0){
                         $validationElement.removeClass('is-hidden').removeClass('is-error').addClass('is-warning');
                         var validationMessage = validationReport.warnings.reduce(function(totalMessage, currentMessage){
                             return totalMessage + currentMessage;
                         }, '');
                         $validationElement.attr('title', validationMessage);
                     }
                 }
            });
        },
        clearValidation: function(){
            var $validationElement = this.$el.find('> .property-label .property-validation');
            this.$el.removeClass('has-warning').removeClass('has-error');
            $validationElement.addClass('is-hidden');

            var elementsToCheck = this.$el.find('.is-bulk > .if-viewing .list-value');
            _.forEach(elementsToCheck, function(element) {
                $validationElement = $(element).find('.cell-validation');
                $validationElement.removeClass('has-warning').removeClass('has-error');
                $validationElement.addClass('is-hidden');
            });
        },
        handleValidation: function(){
            if (this._validationReport){
                this.updateValidation(this._validationReport);
            }
        }
    });
});