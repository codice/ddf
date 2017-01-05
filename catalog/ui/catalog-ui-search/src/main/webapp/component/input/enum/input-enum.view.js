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
    'component/dropdown/dropdown.view'
], function (Marionette, _, $, template, CustomElements, Common, InputView, DropdownView) {

    function getValue(model){
        var multivalued = model.get('property').get('enumMulti');
        var value = model.get('value');
        if (value !== undefined && model.get('property').get('type') === 'DATE'){
            if (multivalued && value.map){
                value = value.map(function(subvalue){
                    return Common.getHumanReadableDate(subvalue);
                });
            } else {
                value = Common.getHumanReadableDate(value);
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
        serializeData: function () {
            var value = getValue(this.model);
            var choice = this.model.get('property').get('enum').filter(function(choice){
                return value.filter(function(subvalue){
                    return JSON.stringify(choice.value) === JSON.stringify(subvalue) || JSON.stringify(choice) === JSON.stringify(subvalue);
                }).length > 0;
            });
            return {
                label: choice ? choice.reduce(function(label, subchoice){
                    label += subchoice.label || subchoice;
                    return label;
                }, '') : value
            };
        },
        onRender: function () {
            this.initializeEnum();
            InputView.prototype.onRender.call(this);
        },
        initializeEnum: function(){
            var value = getValue(this.model);
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
                    defaultSelection: value,
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
            this.enumRegion.currentView.model.set('value', getValue(this.model));
        },
        getCurrentValue: function(){
            var currentValue = this.model.get('property').get('enumMulti') ?
                this.enumRegion.currentView.model.get('value') : this.enumRegion.currentView.model.get('value')[0];
            switch(this.model.getCalculatedType()){
                case 'date':
                    if (currentValue){
                        return (new Date(currentValue)).toISOString();
                    } else {
                        return null;
                    }
                    break;
                default:
                    return currentValue;
            }
        },
        triggerChange: function(){
            this.$el.trigger('change');
        }
    });
});