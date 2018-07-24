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
    './input-enum.hbs',
    'js/CustomElements',
    'js/Common',
    '../input.view',
    'component/dropdown/dropdown.view',
    'moment',
    'component/singletons/user-instance',
    'component/dropdown/dropdown'
], function (Marionette, _, $, template, CustomElements, Common, InputView, DropdownView, moment, user, DropdownModel) {

    function getValue(model){
        var multivalued = model.get('property').get('enumMulti');
        var value = model.get('value');
        if (value !== undefined && model.get('property').get('type') === 'DATE'){
            if (multivalued && value.map){
                value = value.map(function(subvalue){
                    return user.getUserReadableDate(subvalue);
                });
            } else {
                value = user.getUserReadableDate(value);
            }
        }
        if (!multivalued){
            value = [value];
        }
        return value;
    }

    return InputView.extend({
        template: template,
        events: {
            'click .input-revert': 'revert'
        },
        regions: {
            enumRegion: '.enum-region'
        },
        listenForChange: function(){
            this.listenTo(this.enumRegion.currentView.model, 'change:value', function(){
                this.model.set('value', this.getCurrentValue());
                this.validate();
            });
        },
        serializeData: function () {
            var value = getValue(this.model);
            var choice = this.model.get('property').get('enum').filter(function(choice){
                return value.filter(function(subvalue){
                    return JSON.stringify(choice.value) === JSON.stringify(subvalue) || JSON.stringify(choice) === JSON.stringify(subvalue);
                }).length > 0;
            });
            return {
                label: choice.length > 0 ? choice : value
            };
        },
        onRender: function () {
            this.initializeEnum();
            InputView.prototype.onRender.call(this);
        },
        initializeEnum: function(){
            var value = getValue(this.model);
            var dropdownModel = new DropdownModel({
                value: value
            }); 
            const list = this.model.get('property').get('enum').map(function(value){
                if (value.label) {
                    return {
                        label: value.label,
                        value: value.value,
                        class: value.class
                    };
                } else {
                    return {
                        label: value,
                        value: value,
                        class: value
                    };
                }
            });
            if (this.model.get('property').get('enumCustom')) {
                list.unshift( {
                    label: value[0],
                    value: value[0],
                    filterChoice: true
                });
            }
            this.enumRegion.show(DropdownView.createSimpleDropdown(
                {
                    list: list,
                    model: dropdownModel,
                    defaultSelection: value,
                    isMultiSelect: this.model.get('property').get('enumMulti'),
                    hasFiltering: this.model.get('property').get('enumFiltering'),
                    filterChoice: this.model.get('property').get('enumCustom'),
                    matchcase: this.model.get('property').get('matchcase')
                }
            ));
        },
        handleReadOnly: function () {
            this.$el.toggleClass('is-readOnly', this.model.isReadOnly());
        },
        handleValue: function(){
            this.enumRegion.currentView.model.set('value', getValue(this.model));
        },
        getCurrentValue: function(){
            var currentValue = this.model.get('property').get('enumMulti') ?
                this.enumRegion.currentView.model.get('value') : this.enumRegion.currentView.model.get('value')[0];
            switch(this.model.getCalculatedType()){
                case 'date':
                    if (currentValue){
                        return (moment(currentValue)).toISOString();
                    } else {
                        return null;
                    }
                default:
                    return currentValue;
            }
        },
        isValid: function(){
            var value = getValue(this.model);
            var choice = this.model.get('property').get('enum').filter(function(choice){
                return value.filter(function(subvalue){
                    return JSON.stringify(choice.value) === JSON.stringify(subvalue) || JSON.stringify(choice) === JSON.stringify(subvalue);
                }).length > 0;
            });
            return choice.length > 0;
        }
    });
});
