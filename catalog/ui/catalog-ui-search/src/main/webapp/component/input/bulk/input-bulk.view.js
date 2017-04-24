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
        listenForChange: function(){
            this.listenTo(this.enumRegion.currentView.model, 'change:value', function(){
                var value = this.enumRegion.currentView.model.get('value')[0];
                switch(value){
                    case 'bulkDefault':
                        this.model.revert();
                    break;
                    case 'bulkCustom':
                        this.model.setValue(this.otherInput.currentView.model.getValue());
                        break;
                    default:
                        this.model.setValue(value);
                    break;
                }
                this.handleChange();
            });
            this.listenTo(this.otherInput.currentView.model, 'change:value', function(){
                this.model.setValue(this.otherInput.currentView.model.getValue());
                this.handleChange();
            });
        },
        onRender: function () {
            this.initializeDropdown();
            InputView.prototype.onRender.call(this);
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
                    value: 'bulkDefault',
                    help: 'This is the default.  Selecting it will cause no changes to the results, allowing them to keep their multiple values.'
                },
                {
                    label: 'Custom',
                    value: 'bulkCustom',
                    help: 'Select this to enter a custom value.'
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
                    label: label,
                    value: value,
                    hits: valueInfo.hits
                });
            }.bind(this));
            this.enumRegion.show(DropdownView.createSimpleDropdown(
                {
                    list: enumValues,
                    defaultSelection: ['bulkDefault'],
                    hasFiltering: true
                }
            ));
        },
        onBeforeShow: function () {
            this.otherInput.show(new MultivalueView({
                model: this.model.isHomogeneous() ? this.model : this.model.clone() // in most cases this view is the real input, except for the heterogenous case
            }));
            this.otherInput.currentView.listenTo(this.model, 'change:isEditing', function(){
                this.otherInput.currentView.model.set('isEditing', this.model.get('isEditing'));
            }.bind(this));
            if (!this.model.isHomogeneous() && this.model.isMultivalued()){
                this.otherInput.currentView.addNewValue();
            }
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
        }
    });
});