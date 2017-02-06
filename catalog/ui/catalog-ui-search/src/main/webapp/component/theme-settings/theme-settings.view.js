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
var template = require('./theme-settings.hbs');
var CustomElements = require('js/CustomElements');
var user = require('component/singletons/user-instance');
var $ = require('jquery');

function getPreferences(user){
    return user.get('user').get('preferences');
}

function getFontSize(user){
    return getPreferences(user).get('fontSize');
}

function calculatePercentZoom(user){
    var fontSize = getFontSize(user);
    return Math.floor(100*(fontSize/16));
}

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('theme-settings'),
    modelEvents: {},
    events: {
        'click .size-decrease': 'decreaseFontSize',
        'click .size-increase': 'increaseFontSize'
    },
    regions: {},
    ui: {},
    onBeforeShow: function() {},
    decreaseFontSize: function() {
        var preferences = getPreferences(user);
        var currentSize = preferences.get('fontSize');
        preferences.set('fontSize', currentSize - 1);
    },
    increaseFontSize: function() {
        var preferences = getPreferences(user);
        var currentSize = preferences.get('fontSize');
        preferences.set('fontSize', currentSize + 1);
    },
    onRender: function(){
        this.$el.find('.zoom-percent').html(calculatePercentZoom(user));
        this.$el.find('input').val(getFontSize(user));
        this.listenToFontSize();
    },
    listenToFontSize: function(){
        this.$el.find('input').on('change input', function(e){
            var preferences = getPreferences(user);
            var newFontSize = $(e.currentTarget).val();
            preferences.set('fontSize', newFontSize);
            this.$el.find('.zoom-percent').html(calculatePercentZoom(user));
        }.bind(this));
    }
});