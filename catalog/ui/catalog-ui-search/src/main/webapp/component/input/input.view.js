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
    'text!./input.hbs',
    'js/CustomElements'
], function (Marionette, _, $, InputTemplate, CustomElements) {

    var InputView = Marionette.LayoutView.extend({
        template: InputTemplate,
        tagName: CustomElements.register('input'),
        attributes: function(){
            return {
                'data-id': this.model.get('id')
            }
        },
        events: {
            'click .input-revert': 'revert',
            'keyup input': 'handleRevert'
        },
        modelEvents: {
            'change:value': 'render'
        },
        regions: {},
        initialize: function(){
            //console.log('initializing');
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
        handleReadOnly: function () {
            this.$el.toggleClass('is-readOnly', this.model.isReadOnly());
        },
        handleEdit: function () {
            this.$el.toggleClass('is-editing', this._editMode);
        },
        handleValue: function(){
            this.$el.find('input').val(this.model.getValue());
        },
        turnOnEditing: function(){
            this._editMode = true;
            this.handleEdit();
        },
        turnOffEditing: function(){
            this._editMode = false;
            this.handleEdit();
        },
        turnOnLimitedWidth: function(){
            this.$el.addClass('has-limited-width');
        },
        revert: function(){
            this.model.revert();
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
                    values: [this.model.getValue()]
                };
            } else {
                return undefined;
            }
        },
        focus: function(){
            this.$el.find('input').select();
        },
        hasChanged: function(){
            var value = this.$el.find('input').val();
            return value !== this.model.getInitialValue();
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
            var $validationElement = this.$el.find('.input-validation');
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
            } else {
                this.$el.removeClass('has-warning').removeClass('has-error');
                $validationElement.addClass('is-hidden');
            }
        },
        handleValidation: function(){
            if (this._validationReport){
                this.updateValidation(this._validationReport);
            }
        },
        _editMode: false
    });

    return InputView;
});