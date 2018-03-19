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
var $ = require('jquery');
var _ = require('underscore');
var Marionette = require('marionette');
var template = require('./input-with-param.hbs');
var InputView = require('../input.view');

module.exports = InputView.extend({
    template: template,
    className: 'is-with-param',
    getCurrentValue: function(){
        var text = this.$el.find('[type=text]').val();
        var param = parseInt(this.$el.find('[type=number]').val())
        return {
            value: text,
            distance: Math.max(1, param || 0)
        };
    },
    handleValue: function(){
        var value = this.model.getValue() || {
            value: undefined,
            distance: 2
        };
        this.$el.find('[type=text]').val(value.value);
        this.$el.find('[type=number]').val(value.distance);
    },
    serializeData: function () {
        var value = this.model.getValue() || {
            value: undefined,
            distance: 2
        };
        return _.extend(this.model.toJSON(), {
            text: value.value,
            param: value.distance,
            label: this.model.get('property').get('param'),
            help: this.model.get('property').get('help')
        });
    }
});