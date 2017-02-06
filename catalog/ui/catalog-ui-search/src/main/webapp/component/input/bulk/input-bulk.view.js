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
    './input-bulk.hbs',
    'js/CustomElements',
    'component/input/input.view',
    'component/multivalue/multivalue.view',
    'component/dropdown/dropdown.view',
    'js/Common',
    'moment'
], function (Marionette, _, $, template, CustomElements, InputView, MultivalueView, DropdownView, Common, moment) {

    return InputView.extend({
        className: 'is-bulk',
        template: template,
        regions: {
            enumRegion: '.enum-region',
            otherInput: '.input-other'
        },
        events: {
        },
        onRender: function () {
            this.initializeDropdown();
            this.handleEdit();
            this.handleReadOnly();
            this.handleValue();
            this.handleOther();
            this.handleBulk();
        },
        serializeData: function(){
            // need duplicate (usually toJSON returns a side-effect free version, but this has a nested object that isn't using backbone associations)
            var modelJSON = Common.duplicate(this.model.toJSON());
            switch(this.model.getCalculatedType()){
                case 'date':
                    modelJSON.values = _.map(modelJSON.values, (function(valueInfo){
                        valueInfo.value = valueInfo.value.map(function(value){
                            return Common.getHumanReadableDate(value);
                        });
                        return valueInfo;
                    }));
                    break;
                default:
                    break;
            }
            return modelJSON;
        },
        initializeDropdown: function(){
            var enumValues = [
                {
                    label: 'Multiple Values',
                    value: 'bulkDefault'
                }
            ];
            _.forEach( this.model.get('values'), function(valueInfo){
                var value = valueInfo.value;
                var label = value;
                switch(this.model.getCalculatedType()){
                    case 'date':
                        label = label.map(function(text){
                           return Common.getHumanReadableDate(text);
                        });
                        value = value.map(function(text){
                            return moment(text);
                        });
                        break;
                    default:
                        break;
                }
                enumValues.push({
                    label: label + '    ('+valueInfo.hits+')',
                    value: value
                });
            }.bind(this));
            enumValues.push({
                label: 'Other',
                value: 'bulkCustom'
            });
            this.enumRegion.show(DropdownView.createSimpleDropdown(
                {
                    list: enumValues,
                    defaultSelection: ['bulkDefault'],
                    hasFiltering: true
                }
            ));
            this.listenTo(this.enumRegion.currentView.model, 'change:value', this.triggerChange);
        },
        onBeforeShow: function () {
            this.otherInput.show(new MultivalueView({
                model: this.model
            }));
        },
        handleChange: function () {
            this.handleOther();
        },
        handleOther: function () {
            if (this.enumRegion.currentView.model.get('value')[0] === 'bulkCustom') {
                this.$el.addClass('is-other');
            } else {
                this.$el.removeClass('is-other');
            }
        },
        handleBulk: function(){
            if (this.model.isHomogeneous()){
                this.turnOffBulk();
            }
        },
        turnOffBulk: function(){
            this.$el.addClass('is-homogeneous')
        },
        hasChanged: function(){
            if (this.model.isHomogeneous()) {
                return this.otherInput.currentView.hasChanged();
            } else if (this.enumRegion.currentView.model.get('value')[0] === 'bulkDefault') {
                return false;
            } else {
                return true;
            }
        },
        getCurrentValue: function(){
            if (this.model.isHomogeneous()) {
                return this.otherInput.currentView.getCurrentValue();
            } else if (this.enumRegion.currentView.model.get('value')[0] === 'bulkDefault') {
                return false;
            } else if (this.enumRegion.currentView.model.get('value')[0] === 'bulkCustom') {
                return this.otherInput.currentView.getCurrentValue();
            } else {
                return this.enumRegion.currentView.model.get('value')[0];
            }
        },
        triggerChange: function(){
            this.handleChange();
            this.$el.trigger('change');
        }
    });
});