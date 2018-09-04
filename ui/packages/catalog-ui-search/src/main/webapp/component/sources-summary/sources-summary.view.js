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
var CustomElements = require('js/CustomElements');
var template = require('./sources-summary.hbs');
var sources = require('component/singletons/sources-instance');

module.exports = Marionette.ItemView.extend({
    template: template,
    tagName: CustomElements.register('sources-summary'),
    model: sources,
    modelEvents: {
        'all': 'render' 
    },
    events: {
    },
    initialize: function(){
        
    },
    onRender: function(){
        this.$el.toggleClass('is-available', this.model.get('available'));
    },
    serializeData: function(){
        var downSources = this.model.filter(function(source){
            return !source.get('available');
        });
        if (downSources.length > 0){
            return {
                down: true,
                single: downSources.length === 1,
                amountDown: downSources.length
            };
        } else {
            return {
                down: false
            };
        }
    }
});