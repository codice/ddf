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
    '../input.view',
    'bootstrapDatepicker'
], function (Marionette, _, $, template, CustomElements, moment, InputView) {

    var format = 'DD MMM YYYY HH:mm:ss.SSS';
    function getHumanReadableDate(date) {
        return moment(date).format(format);
    }

    return InputView.extend({
        template: template,
        events: {
            'click .input-revert': 'revert',
            'dp.change .input-group.date': 'handleRevert',
            'dp.show .input-group.date': 'handleOpen',
            'dp.hide .input-group.date': 'removeResizeHandler'
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
            InputView.prototype.onRender.call(this);
        },
        initializeDatepicker: function(){
            this.$el.find('.input-group.date').datetimepicker({
                format: format,
                widgetParent: 'body'
            });
        },
        handleReadOnly: function () {
            this.$el.toggleClass('is-readOnly', this.model.isReadOnly());
        },
        handleValue: function(){
            this.$el.find('.input-group.date').data('DateTimePicker').date(getHumanReadableDate(this.model.getValue()));
        },
        save: function(){
            var value = this.$el.find('input').val();
            this.model.save(moment(value).toJSON());
        },
        focus: function(){
            this.$el.find('input').select();
        },
        hasChanged: function(){
            var value = this.$el.find('input').val();
            return value !== getHumanReadableDate(this.model.getInitialValue());
        },
        handleOpen: function(){
            this.updatePosition();
            this.addResizeHandler();
        },
        updatePosition: function(){
            var inputCoordinates = this.$el.find('.input-group.date')[0].getBoundingClientRect();
            $('body > .bootstrap-datetimepicker-widget').css('left', inputCoordinates.left)
                .css('top', inputCoordinates.top + inputCoordinates.height)
                .css('width', inputCoordinates.width);
        },
        addResizeHandler: function(){
            $(window).on('resize.datePicker', this.updatePosition.bind(this));
        },
        removeResizeHandler: function(){
            $(window).off('resize.datePicker');
        },
        getCurrentValue: function(){
            var currentValue = this.$el.find('input').val();
            if (currentValue){
                return (new Date(this.$el.find('input').val())).getTime();
            } else {
                return null;
            }
        }
    });
});