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
    'text!./input-date.hbs',
    'js/CustomElements',
    'moment',
    'bootstrapDatepicker',
], function (Marionette, _, $, template, CustomElements, moment) {

    var format = 'DD MMM YYYY HH:mm:ss.SSS';
    function getHumanReadableDate(date) {
        return moment(date).format(format);
    }

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('input-date'),
        attributes: function(){
            return {
                'data-id': this.model.get('id')
            }
        },
        events: {
            'click .input-revert': 'revert',
            'click button': 'upload'
        },
        modelEvents: {
            'change:value': 'render'
        },
        regions: {},
        serializeData: function () {
            return _.extend(this.model.toJSON(), {
                cid: this.cid,
                humanReadableDate: getHumanReadableDate(this.model.getValue())
            });
        },
        onRender: function () {
            this.initializeDatepicker();
            this.handleEdit();
            this.handleReadOnly();
            this.handleValue();
            this.handleRevert();
        },
        initializeDatepicker: function(){
            this.$el.find('.input-group.date').datetimepicker({
                format: format,
                debug: true
            });
        },
        handleReadOnly: function () {
            this.$el.toggleClass('is-readOnly', this.model.isReadOnly());
        },
        handleEdit: function () {
            this.$el.toggleClass('is-editing', this._editMode);
        },
        handleValue: function(){
            this.$el.find('.input-group.date').data('DateTimePicker').date(getHumanReadableDate(this.model.getValue()));
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
        focus: function(){
            this.$el.find('input').select();
        },
        handleRevert: function(){
            var value = this.$el.find('input').val();
            console.log(moment(value).format());
            if (value !== this.model.getInitialValue()){
                this.$el.addClass('is-changed');
            } else {
                this.$el.removeClass('is-changed');
            }
        },
        upload: function(){
            this.$el.find('input').click();
        },
        _editMode: false
    });
});