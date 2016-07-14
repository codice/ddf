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
    'text!./input-enum.hbs',
    'js/CustomElements',
    'moment',
    '../input.view',
    'component/dropdown/dropdown.view'
], function (Marionette, _, $, template, CustomElements, moment, InputView, DropdownView) {

    var format = 'DD MMM YYYY HH:mm:ss.SSS';
    function getHumanReadableDate(date) {
        return moment(date).format(format);
    }

    return InputView.extend({
        template: template,
        events: {
            'click .input-revert': 'revert'
        },
        regions: {
            enumRegion: '.enum-region'
        },
        serializeData: function () {
            var value = this.model.get('value');
            switch(this.model.getCalculatedType()){
                case 'date':
                    value = getHumanReadableDate(value);
                    break;
                default:
                    break;
            }
            var choice = this.model.get('property').get('enum').filter(function(choice){
                return JSON.stringify(choice.value) === JSON.stringify(value) || JSON.stringify(choice) === JSON.stringify(value);
            })[0];
            return {
                label: choice ? choice.label || choice : value
            };
        },
        onRender: function () {
            this.initializeEnum()
            InputView.prototype.onRender.call(this);
        },
        initializeEnum: function(){
            this.enumRegion.show(DropdownView.createSimpleDropdown(
                {
                    list: this.model.get('property').get('enum').map(function(value){
                        if (value.label) {
                            return {
                                label: value.label,
                                value: value.value
                            }
                        } else {
                            return {
                                label: value,
                                value: value
                            };
                        }
                    }),
                    defaultSelection: this.model.get('property').get('enumMulti') ? this.model.get('value') : [this.model.get('value')],
                    isMultiSelect: this.model.get('property').get('enumMulti'),
                    hasFiltering: this.model.get('property').get('enumFiltering')
                }
            ));
            this.listenTo(this.enumRegion.currentView.model, 'change:value', this.triggerChange);
        },
        handleReadOnly: function () {
            this.$el.toggleClass('is-readOnly', this.model.isReadOnly());
        },
        handleValue: function(){
            var value = this.model.get('value');
            if (value && !this.model.get('property').get('enumMulti')){
                value = [value];
            } else if (!value) {
                value = [];
            }
            this.enumRegion.currentView.model.set('value', value);
        },
        save: function(){
            /*var value = this.$el.find('input').val();
            this.model.save(moment(value).toJSON());*/
        },
        focus: function(){
           // this.$el.find('input').select();
        },
        hasChanged: function(){
            var value = this.enumRegion.currentView.model.get('value')[0];
            switch(this.model.getCalculatedType()){
                case 'date':
                    return value !== getHumanReadableDate(this.model.getInitialValue());
                    break;
                default:
                    return value !== this.model.getInitialValue();
                    break;
            }
        },
        getCurrentValue: function(){
            var currentValue = this.model.get('property').get('enumMulti') ?
                this.enumRegion.currentView.model.get('value') : this.enumRegion.currentView.model.get('value')[0];
            switch(this.model.getCalculatedType()){
                case 'date':
                    if (currentValue){
                        return (new Date(this.$el.find('input').val())).toISOString();
                    } else {
                        return null;
                    }
                    break;
                default:
                    return currentValue;
                    break;
            }
        },
        triggerChange: function(){
            this.$el.trigger('change');
        }
    });
});