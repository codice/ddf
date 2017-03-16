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
var PropertyView = require('component/property/property.view');
var Property = require('component/property/property');

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

function calculateFontSize(percentage){
    return (percentage * 16) / 100;
}

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('theme-settings'),
    regions: {
        fontSize: '.theme-font-size'
    },
    onBeforeShow: function() {
        this.fontSize.show(new PropertyView({
            model: new Property({
                label: 'Zoom Percentage',
                value: [calculatePercentZoom(user)],
                min: 62,
                max: 200,
                units: '%',
                type: 'RANGE'
            })
        }));
        this.fontSize.currentView.turnOnLimitedWidth();
        this.fontSize.currentView.turnOnEditing();
        this.$el.on('change keyup mouseup revert', this.handleEvent.bind(this));
    },
    handleEvent: function(e){
        switch(e.target.type){
            case 'range':
                if (e.type === 'mouseup' || e.type === 'keyup') {
                    this.saveChanges();
                }
            break;
            case 'number':
                if (e.type === 'change'){
                    this.saveChanges();
                }
            break;
            default:
                if (e.type === 'revert'){
                    this.saveChanges();
                }
            break;
        }
    },
    saveChanges: function(){
        var preferences = getPreferences(user);
        var newFontSize = this.fontSize.currentView.getCurrentValue()[0];
        preferences.set('fontSize', calculateFontSize(newFontSize));
    }
});