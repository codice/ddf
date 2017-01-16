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
var template = require('./system-usage.hbs');
var CustomElements = require('js/CustomElements');
var properties = require('properties');
var user = require('component/singletons/user-instance.js');
var preferences = user.get('user').get('preferences');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('system-usage'),
    events: {
        'click button': 'handleClick'
    },
    initialize: function(){
        this.listenTo(preferences, 'change:fontSize', function(){
            this.render();
        }.bind(this));
    },
    serializeData: function(){
        return {
            fontSize: preferences.get('fontSize'),
            properties: properties   
        };
    },
    handleClick: function(){
        this.$el.trigger(CustomElements.getNamespace() + 'close-lightbox');
    }
});