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
/*global define, alert, window*/
define([
    'marionette',
    'underscore',
    'jquery',
    './input-date.hbs',
    'js/CustomElements',
    'moment-timezone',
    '../input.view',
    'js/Common',
    'component/singletons/user-instance',
    'eonasdan-bootstrap-datetimepicker'
], function (Marionette, _, $, template, CustomElements, moment, InputView, Common, user) {

    function getDateFormat() {
        return user.get('user').get('preferences').get('timeFormat');
    }

    function getTimeZone() {
        return user.get('user').get('preferences').get('timeZone');
    }

    return InputView.extend({
        template: template,
        events: {
            'click .input-revert': 'revert',
            'dp.change .input-group.date': 'handleRevert',
            'dp.show .input-group.date': 'handleOpen',
            'dp.hide .input-group.date': 'removeResizeHandler'
        },
        serializeData: function () {
            const propertyJSON = _.extend(this.model.toJSON(), {
                cid: this.cid,
                humanReadableDate: this.model.getValue() ? user.getUserReadableDate(this.model.getValue()) : this.model.getValue()
            });
            if (propertyJSON.property.placeholder === undefined) {
                propertyJSON.property.placeholder = getDateFormat();
            }
            return propertyJSON;
        },
        onRender: function () {
            this.initializeDatepicker();
            InputView.prototype.onRender.call(this);
        },
        initializeDatepicker: function(){
            this.$el.find('.input-group.date').datetimepicker({
                format: getDateFormat(),
                timeZone: getTimeZone(),
                widgetParent: 'body',
                keyBinds: null
            });
        },
        handleReadOnly: function () {
            this.$el.toggleClass('is-readOnly', this.model.isReadOnly());
        },
        handleValue: function(){
            this.$el.find('.input-group.date').data('DateTimePicker').date(user.getUserReadableDate(this.model.getValue()));
        },
        focus: function(){
            this.$el.find('input').select();
        },
        handleOpen: function(){
            this.updatePosition();
            this.addResizeHandler();
        },
        updatePosition: function () {
            let datepicker = $('body').find('.bootstrap-datetimepicker-widget:last');
            let inputCoordinates = this.$el.find('.input-group.date')[0].getBoundingClientRect();
            let top = datepicker.hasClass('bottom') ? inputCoordinates.top + inputCoordinates.height : inputCoordinates.top - datepicker.outerHeight();
            datepicker.css({
                'top': top + 'px',
                'bottom': 'auto',
                'left': inputCoordinates.left + 'px',
                'width': inputCoordinates.width + 'px'
            });
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
                return (moment.tz(currentValue, getDateFormat(), getTimeZone())).toISOString();
            } else {
                return null;
            }
        },
        listenForChange: function(){
            this.$el.on('dp.change click input change keyup', function(){
                this.model.set('value', this.getCurrentValue());
            }.bind(this));

        },
        isValid: function(){
            var currentValue = this.$el.find('input').val();
            return currentValue != null && currentValue !== '';
        },
        onDestroy: function(){
            var datetimepicker = this.$el.find('.input-group.date').data('DateTimePicker');
            if (datetimepicker) {
                datetimepicker.destroy();
            }
        }
    });
});
