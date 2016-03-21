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
    'text!./input-query-time.hbs',
    'js/CustomElements',
    '../input.view'
], function (Marionette, _, $, InputTemplate, CustomElements, InputView) {

    var QueryTimeInputView = InputView.extend({
        template: InputTemplate,
        tagName: CustomElements.register('input-query-time'),
        events: {},
        modelEvents: {
            'change:value': 'render'
        },
        regions: {},
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
        revert: function(){
            this.model.revert();
        },
        save: function(){
            var value = this.$el.find('input').val();
            this.model.save(value);
        },
        focus: function(){
            this.$el.find('input').select();
        },
        _editMode: false
    });

    return QueryTimeInputView;
});