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
    './input-radio.hbs',
    'js/CustomElements',
    '../input.view',
    'component/radio/radio.view'
], function (Marionette, _, $, template, CustomElements, InputView, RadioView) {

    return InputView.extend({
        template: template,
        events: {
            'click .input-revert': 'revert'
        },
        regions: {
            radioRegion: '.radio-region'
        },
        listenForChange: function(){
            this.$el.on('click', function(){
                this.model.set('value', this.getCurrentValue());
            }.bind(this));
        },
        serializeData: function () {
            var value = this.model.get('value');
            var choice = this.model.get('property').get('radio').filter(function(choice){
                return JSON.stringify(choice.value) === JSON.stringify(value) || JSON.stringify(choice) === JSON.stringify(value);
            })[0];
            return {
                label: choice ? choice.label : value
            };
        },
        onRender: function () {
            this.initializeRadio();
            InputView.prototype.onRender.call(this);
        },
        initializeRadio: function(){
            this.radioRegion.show(RadioView.createRadio(
                {
                    options: this.model.get('property').get('radio').map(function(value){
                        if (value.label) {
                            return {
                                label: value.label,
                                value: value.value
                            };
                        } else {
                            return {
                                label: value,
                                value: value
                            };
                        }
                    }),
                    defaultValue: [this.model.get('value')]
                }
            ));
        },
        handleReadOnly: function () {
            this.$el.toggleClass('is-readOnly', this.model.isReadOnly());
        },
        handleValue: function(){
            this.radioRegion.currentView.model.set('value', this.model.get('value'));
        },
        getCurrentValue: function(){
            var currentValue = this.radioRegion.currentView.model.get('value');
            return currentValue;
        }
    });
});