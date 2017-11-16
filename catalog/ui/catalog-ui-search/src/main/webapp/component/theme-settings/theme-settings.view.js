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
var ThemeUtils = require('js/ThemeUtils');

function getPreferences(user){
    return user.get('user').get('preferences');
}

function getFontSize(user){
    return getPreferences(user).get('fontSize');
}

function getSpacingMode(user){
    return getPreferences(user).get('theme').getSpacingMode();
}

function getAnimationMode(user){
    return getPreferences(user).get('animation');
}

function getTheme(user){
    return getPreferences(user).get('theme').getColorMode();
}

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('theme-settings'),
    regions: {
        fontSize: '.theme-font-size',
        spacingMode: '.theme-spacing-mode',
        theme: '.theme-theme',
        animationMode: '.theme-animation',
        customPrimaryColor: '.theme-customPrimaryColor',
        customPositiveColor: '.theme-customPositiveColor',
        customNegativeColor: '.theme-customNegativeColor',
        customWarningColor: '.theme-customWarningColor',
        customFavoriteColor: '.theme-customFavoriteColor',
        customBackgroundNavigation: '.theme-customBackgroundNavigation',
        customBackgroundAccentContent: '.theme-customBackgroundAccentContent',
        customBackgroundDropdown: '.theme-customBackgroundDropdown',
        customBackgroundContent: '.theme-customBackgroundContent',
        customBackgroundModal: '.theme-customBackgroundModal',
        customBackgroundSlideout: '.theme-customBackgroundSlideout'
    },
    onBeforeShow: function() {
        this.showFontSize();
        this.showSpacingMode();
        this.showAnimation();
        this.showTheme();
        this.showCustomColors();
        this.handleTheme();
        this.listenTo(getPreferences(user).get('theme'), 'change:theme', this.handleTheme);
    },
    handleTheme: function() {
        var theme = getTheme(user);
        this.$el.toggleClass('has-custom-theme', theme === 'custom');
    },
    showCustomColors: function() {
        var customColors = getPreferences(user).get('theme').getCustomColorNames();
        customColors.forEach((colorVariable) => {
            this.$el.find('.theme-custom').append('<div class="theme-'+colorVariable+'"></div>');
            this.addRegion(colorVariable, '.theme-'+colorVariable);
            var propertyModel = new Property({
                label: colorVariable.replace(/([A-Z])/g, ' $1').replace(/^./, function(str){ return str.toUpperCase(); }).substring(7),
                value: [getPreferences(user).get('theme').get(colorVariable)],
                type: 'COLOR'
            });
            this[colorVariable].show(new PropertyView({
                model: propertyModel
            }));
            this[colorVariable].currentView.turnOnLimitedWidth();
            this[colorVariable].currentView.turnOnEditing();
            this.listenTo(propertyModel, 'change:value', () => {
                var preferences = getPreferences(user);
                var newValue = propertyModel.getValue()[0];
                preferences.get('theme').set(colorVariable, newValue);
                getPreferences(user).savePreferences();
            });
        });
    },
    showAnimation: function(){
        var animationModel = new Property({
            label: 'Animation',
            value: [getAnimationMode(user)],
            enum: [
                {
                    label: 'On',
                    value: true
                },
                {
                    label: 'Off',
                    value: false
                }
            ],
            id: 'Animation'
        });
        this.animationMode.show(new PropertyView({
            model: animationModel
        }));
        this.animationMode.currentView.turnOnLimitedWidth();
        this.animationMode.currentView.turnOnEditing();
        this.listenTo(animationModel, 'change:value', this.saveAnimationChanges);
    },
    showFontSize: function(){
        var fontSizeModel = new Property({
            label: 'Zoom Percentage',
            value: [ThemeUtils.getZoomScale(getFontSize(user))],
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
    showTheme: function(){
        var themeModel = new Property({
            enum: [
                {
                    label: 'Dark',
                    value: 'dark'
                },
                {
                    label: 'Light',
                    value: 'light'
                },
                {
                    label: 'Sea',
                    value: 'sea'
                },
                {
                    label: 'Custom',
                    value: 'custom'
                }
            ],
            value: [getTheme(user)],
            id: 'Theme'
        });
        this.theme.show(new PropertyView({
            model: themeModel
        }));
        this.theme.currentView.turnOnLimitedWidth();
        this.theme.currentView.turnOnEditing();
        this.listenTo(themeModel, 'change:value', this.saveThemeChanges);
    },
    saveFontChanges: function(){
        var preferences = getPreferences(user);
        var newFontSize = this.fontSize.currentView.model.getValue()[0];
        preferences.set('fontSize', ThemeUtils.getFontSize(newFontSize));
    },
    saveAnimationChanges: function(){
        var preferences = getPreferences(user);
        var newAnimationMode = this.animationMode.currentView.model.getValue()[0];
        preferences.set('animation', newAnimationMode);
        getPreferences(user).savePreferences();
    },  
    saveSpacingChanges: function(){
        var preferences = getPreferences(user);
        var newSpacingMode = this.spacingMode.currentView.model.getValue()[0];
        preferences.get('theme').set('spacingMode', newSpacingMode);
        getPreferences(user).savePreferences();
    },
    saveThemeChanges: function() {
        var preferences = getPreferences(user);
        var newTheme = this.theme.currentView.model.getValue()[0];
        preferences.get('theme').set('theme', newTheme);
        getPreferences(user).savePreferences();
    },
    saveChanges: function(){
        this.saveFontChanges();
        this.saveSpacingChanges();
    }
});