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
        className: function(){
            return 'is-'+this.model.getCalculatedType();
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
            }
        },
        serializeData: function () {
            return _.extend(this.model.toJSON(), {cid: this.cid});
        },
        onRender: function () {
            this.handleEdit();
            this.handleReadOnly();
            this.handleValue();
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
        toPatchJSON: function(){
            if (this.hasChanged()){
                return {
                    attribute: this.model.getId(),
                    values: [this.model.getCurrentValue()]
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
        getCurrentValue: function(){
            return this.$el.find('input').val();
        }
    });

    return InputView;
});