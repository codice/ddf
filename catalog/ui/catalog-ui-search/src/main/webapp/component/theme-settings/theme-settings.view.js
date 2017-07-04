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

function getSpacingMode(user){
    return getPreferences(user).get('theme').getSpacingMode();
}

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('theme-settings'),
    regions: {
        fontSize: '.theme-font-size',
        spacingMode: '.theme-spacing-mode'
    },
    onBeforeShow: function() {
        this.showFontSize();
        this.showSpacingMode();
    },
    showFontSize: function(){
        var fontSizeModel = new Property({
            label: 'Zoom Percentage',
            value: [calculatePercentZoom(user)],
            min: 62,
            max: 200,
            units: '%',
            type: 'RANGE'
        });
        this.fontSize.show(new PropertyView({
            model: fontSizeModel
        }));
        this.fontSize.currentView.turnOnLimitedWidth();
        this.fontSize.currentView.turnOnEditing();
        this.listenTo(fontSizeModel, 'change:value', this.saveFontChanges);
    },
    showSpacingMode: function(){
        var spacingModeModel = new Property({
            enum: [
                {
                    label: 'Comfortable',
                    value: 'comfortable'
                },
                {
                    label: 'Cozy',
                    value: 'cozy'
                },
                {
                    label: 'Compact',
                    value: 'compact'
                }
            ],
            value: [getSpacingMode(user)],
            id: 'Spacing'
        });
        this.spacingMode.show(new PropertyView({
            model: spacingModeModel
        }));
        this.spacingMode.currentView.turnOnLimitedWidth();
        this.spacingMode.currentView.turnOnEditing();
        this.listenTo(spacingModeModel, 'change:value', this.saveSpacingChanges);
    },
    saveFontChanges: function(){
        var preferences = getPreferences(user);
        var newFontSize = this.fontSize.currentView.model.getValue()[0];
        preferences.set('fontSize', calculateFontSize(newFontSize));
    },
    saveSpacingChanges: function(){
        var preferences = getPreferences(user);
        var newSpacingMode = this.spacingMode.currentView.model.getValue()[0];
        preferences.get('theme').set('spacingMode', newSpacingMode);
        getPreferences(user).savePreferences();
    },  
    saveChanges: function(){
        this.saveFontChanges();
        this.saveSpacingChanges();
    }
});