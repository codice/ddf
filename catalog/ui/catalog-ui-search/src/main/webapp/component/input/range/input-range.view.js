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
/*global require*/
var Marionette = require('marionette');
var template = require('./input-range.hbs');
var InputView = require('../input.view');

module.exports = InputView.extend({
    template: template,
    events: {
        'click .units-label': 'triggerFocus'
    },
    triggerFocus: function(){
        this.$el.find('input[type=number]').focus();
    },
    getCurrentValue: function(){
        return Number(this.$el.find('input').val());
    },
    onRender: function(){
        this.listenToRange();
        this.listenToInput();
        InputView.prototype.onRender.call(this);
    },
    adjustValue: function(e){
        var value = this.$el.find('input[type=number]').val();
        var max = this.model.get('property').get('max');
        var min = this.model.get('property').get('min');
        if (value > max){
            value = max;
            this.$el.find('input[type=number]').val(value);
        } else if (e.type === 'change' && value < min){
            value = min;
            this.$el.find('input[type=number]').val(value);
        }
        this.$el.find('.units-value').html(value);
        this.$el.find('input[type=range]').val(value);
    },
    listenToInput: function(){
        this.$el.find('input[type=number]').off('change.range input.range').on('change.range input.range', this.adjustValue.bind(this));
    },
    listenToRange: function(){
        this.$el.find('input[type=range]').off('change.range input.range').on('change.range input.range', function(e){
            var value = this.$el.find('input[type=range]').val();
            this.$el.find('input[type=number]').val(value);
            this.$el.find('.units-value').html(value);
        }.bind(this));
    }
})