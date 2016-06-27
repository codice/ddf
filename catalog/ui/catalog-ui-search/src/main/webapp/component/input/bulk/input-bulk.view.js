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
    'text!./input-bulk.hbs',
    'js/CustomElements',
    'component/input/input.view',
    'component/multivalue/multivalue.view',
    'component/dropdown/dropdown.view',
    'moment'
], function (Marionette, _, $, template, CustomElements, InputView, MultivalueView, DropdownView, moment) {

    var format = 'DD MMM YYYY HH:mm:ss.SSS';
    function getHumanReadableDate(date) {
        return moment(date).format(format);
    }

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
        initializeDropdown: function(){
            var enumValues = [
                {
                    label: 'Multiple Values',
                    value: 'bulkDefault'
                }
            ];
            _.forEach( this.model.get('values'), function(value){
                var label = value.value;
                switch(this.model.getCalculatedType()){
                    case 'date':
                        label = label.map(function(text){
                           return getHumanReadableDate(text);
                        });
                        break;
                    default:
                        break;
                }
                enumValues.push({
                    label: label + '    ('+value.hits+')',
                    value: value.value
                });
            }.bind(this));
            enumValues.push({
                label: 'Other',
                value: 'bulkCustom'
            });
            this.enumRegion.show(DropdownView.createSimpleDropdown(
                enumValues, false, ['bulkDefault']
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