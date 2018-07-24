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
    './input.hbs',
    'js/CustomElements'
], function (Marionette, _, $, InputTemplate, CustomElements) {

    var InputView = Marionette.LayoutView.extend({
        className: function(){
            if (!this.model.get('property').get('enum')){
                return 'is-'+this.model.getCalculatedType();
            } else {
                return 'is-enum';
            }
        },
        template: InputTemplate,
        tagName: CustomElements.register('input'),
        attributes: function(){
            return {
                'data-id': this.model.getId()
            }
        },
        modelEvents: {
            'change:isEditing': 'handleEdit'
        },
        regions: {},
        initialize: function(){
            if (this.model.get('property')){
                this.listenTo(this.model.get('property'), 'change:isEditing', this.handleEdit);
                this.listenTo(this.model, 'change:isValid', this.handleValidation);
            }
        },
        serializeData: function () {
            return _.extend(this.model.toJSON(), {cid: this.cid});
        },
        onRender: function () {
            this.handleEdit();
            this.handleReadOnly();
            this.handleValue();
            this.validate();
        },
        onAttach: function() {
            this.listenForChange();
        },
        listenForChange: function(){
            this.$el.on('change keyup input', function(){
                this.model.set('value', this.getCurrentValue());
                this.validate();
            }.bind(this));
        },
        validate() {
            if (this.model.get('property')) {
                this.model.setIsValid(this.isValid());
            }
        },
        handleValidation: function(){
            if (this.model.showValidationIssues()){
                this.$el.toggleClass('has-validation-issues', !this.model.isValid());
            }
        },
        isValid: function(){
            return true; //overwrite on a per input basis   
        },
        handleReadOnly: function () {
            this.$el.toggleClass('is-readOnly', this.model.isReadOnly());
        },
        handleEdit: function () {
            this.$el.toggleClass('is-editing', this.model.isEditing());
        },
        handleValue: function(){
            this.$el.find('input').val(this.model.getValue());
        },
        toJSON: function(){
            var attributeToVal = {};
            attributeToVal[this.model.getId()] = this.model.getValue();
            return attributeToVal;
        },
        focus: function(){
            this.$el.find('input').select();
        },
        hasChanged: function(){
            var value = this.$el.find('input').val();
            return value !== this.model.getInitialValue();
        },
        getCurrentValue: function(){
            return this.$el.find('input').val();
        }
    });

    return InputView;
});