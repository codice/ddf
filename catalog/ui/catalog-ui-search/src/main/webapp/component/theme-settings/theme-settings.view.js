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

function getBackgroundColor(user){
    return getPreferences(user).get('theme').getBackgroundColor();
}

function getTextColor(user){
    return getPreferences(user).get('theme').getTextColor();
}

function getButtonTextColor(user){
    return getPreferences(user).get('theme').getButtonTextColor();
}

function getPrimaryColor(user){
    return getPreferences(user).get('theme').getPrimaryColor();
}

function getPositiveColor(user){
    return getPreferences(user).get('theme').getPositiveColor();
}

function getNegativeColor(user){
    return getPreferences(user).get('theme').getNegativeColor();
}

function getWarningColor(user){
    return getPreferences(user).get('theme').getWarningColor();
}

function getFavoriteColor(user){
    return getPreferences(user).get('theme').getFavoriteColor();
}

function getLinksVisitedColor(user){
    return getPreferences(user).get('theme').getLinksVisitedColor();
}

function getLinksColor(user){
    return getPreferences(user).get('theme').getLinksColor();
}

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('theme-settings'),
    regions: {
        fontSize: '.theme-font-size',
        spacingMode: '.theme-spacing-mode',
        animationMode: '.theme-animation',
        backgroundColor: '.theme-background-color',
        textColor: '.theme-text-color',
        buttonTextColor: '.theme-button-text-color',
        primaryColor: '.theme-primary-color',
        positiveColor: '.theme-positive-color',
        negativeColor: '.theme-negative-color',
        warningColor: '.theme-warning-color',
        favoriteColor: '.theme-favorite-color',
        linksVisitedColor: '.theme-links-visited-color',
        linksColor: '.theme-links-color'
    },
    onBeforeShow: function() {
        this.showFontSize();
        this.showSpacingMode();
        this.showAnimation();
        this.showBackgroundColor();
        this.showTextColor();
        this.showButtonTextColor();
        this.showPrimaryColor();
        this.showPositiveColor();
        this.showNegativeColor();
        this.showWarningColor();
        this.showFavoritColor();
        this.showLinksVisitedColor();
        this.showLinksColor();
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
    showBackgroundColor: function() {
        var backgroundColorModel = new Property({
            label: 'Base Background Color',
            value: [getBackgroundColor(user)]
        });
        this.backgroundColor.show(new PropertyView({
            model: backgroundColorModel
        }));
        this.backgroundColor.currentView.turnOnLimitedWidth();
        this.backgroundColor.currentView.turnOnEditing();
        this.listenTo(backgroundColorModel, 'change:value', this.saveBackgroundColorChanges);
    },
    showTextColor: function() {
        var textColorModel = new Property({
            label: 'Text Color',
            value: [getTextColor(user)]
        });
        this.textColor.show(new PropertyView({
            model: textColorModel
        }));
        this.textColor.currentView.turnOnLimitedWidth();
        this.textColor.currentView.turnOnEditing();
        this.listenTo(textColorModel, 'change:value', this.saveTextColorChanges);
    },
    showButtonTextColor: function() {
        var buttonTextColorModel = new Property({
            label: 'Button Text Color',
            value: [getButtonTextColor(user)]
        });
        this.buttonTextColor.show(new PropertyView({
            model: buttonTextColorModel
        }));
        this.buttonTextColor.currentView.turnOnLimitedWidth();
        this.buttonTextColor.currentView.turnOnEditing();
        this.listenTo(buttonTextColorModel, 'change:value', this.saveButtonTextColorChanges);
    },
    showPrimaryColor: function() {
        var primaryColorModel = new Property({
            label: 'Primary Color',
            value: [getPrimaryColor(user)]
        });
        this.primaryColor.show(new PropertyView({
            model: primaryColorModel
        }));
        this.primaryColor.currentView.turnOnLimitedWidth();
        this.primaryColor.currentView.turnOnEditing();
        this.listenTo(primaryColorModel, 'change:value', this.savePrimaryColorChanges);
    },
    showPositiveColor: function() {
        var positiveColorModel = new Property({
            label: 'Positive Color',
            value: [getPositiveColor(user)]
        });
        this.positiveColor.show(new PropertyView({
            model: positiveColorModel
        }));
        this.positiveColor.currentView.turnOnLimitedWidth();
        this.positiveColor.currentView.turnOnEditing();
        this.listenTo(positiveColorModel, 'change:value', this.savePositiveColorChanges);
    },
    showNegativeColor: function() {
        var negativeColorModel = new Property({
            label: 'Negative Color',
            value: [getNegativeColor(user)]
        });
        this.negativeColor.show(new PropertyView({
            model: negativeColorModel
        }));
        this.negativeColor.currentView.turnOnLimitedWidth();
        this.negativeColor.currentView.turnOnEditing();
        this.listenTo(negativeColorModel, 'change:value', this.saveNegativeColorChanges);
    },
    showWarningColor: function() {
        var warningColorModel = new Property({
            label: 'Warning Color',
            value: [getWarningColor(user)]
        });
        this.warningColor.show(new PropertyView({
            model: warningColorModel
        }));
        this.warningColor.currentView.turnOnLimitedWidth();
        this.warningColor.currentView.turnOnEditing();
        this.listenTo(warningColorModel, 'change:value', this.saveWarningColorChanges);
    },
    showFavoritColor: function() {
        var favoriteColorModel = new Property({
            label: 'Favorite Color',
            value: [getFavoriteColor(user)]
        });
        this.favoriteColor.show(new PropertyView({
            model: favoriteColorModel
        }));
        this.favoriteColor.currentView.turnOnLimitedWidth();
        this.favoriteColor.currentView.turnOnEditing();
        this.listenTo(favoriteColorModel, 'change:value', this.saveFavoriteColorChanges);
    },
    showLinksVisitedColor: function() {
        var linksVisitedColorModel = new Property({
            label: 'Links Visited Color',
            value: [getLinksVisitedColor(user)]
        });
        this.linksVisitedColor.show(new PropertyView({
            model: linksVisitedColorModel
        }));
        this.linksVisitedColor.currentView.turnOnLimitedWidth();
        this.linksVisitedColor.currentView.turnOnEditing();
        this.listenTo(linksVisitedColorModel, 'change:value', this.saveLinksVisitedColorChanges);
    },
    showLinksColor: function() {
        var linksColorModel = new Property({
            label: 'Links Color',
            value: [getLinksColor(user)]
        });
        this.linksColor.show(new PropertyView({
            model: linksColorModel
        }));
        this.linksColor.currentView.turnOnLimitedWidth();
        this.linksColor.currentView.turnOnEditing();
        this.listenTo(linksColorModel, 'change:value', this.saveLinksColorChanges);
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
    saveBackgroundColorChanges: function(){
        var preferences = getPreferences(user);
        var newBackgroundColor = this.backgroundColor.currentView.model.getValue()[0];
        preferences.get('theme').set('backgroundColor', newBackgroundColor);
        getPreferences(user).savePreferences();
    },
    saveTextColorChanges: function(){
        var preferences = getPreferences(user);
        var newTextColor = this.textColor.currentView.model.getValue()[0];
        preferences.get('theme').set('textColor', newTextColor);
        getPreferences(user).savePreferences();
    },
    saveButtonTextColorChanges: function(){
        var preferences = getPreferences(user);
        var newButtonTextColor = this.buttonTextColor.currentView.model.getValue()[0];
        preferences.get('theme').set('buttonTextColor', newButtonTextColor);
        getPreferences(user).savePreferences();
    },
    savePrimaryColorChanges: function(){
        var preferences = getPreferences(user);
        var newPrimaryColor = this.primaryColor.currentView.model.getValue()[0];
        preferences.get('theme').set('primaryColor', newPrimaryColor);
        getPreferences(user).savePreferences();
    },
    savePositiveColorChanges: function(){
        var preferences = getPreferences(user);
        var newPositiveColor = this.positiveColor.currentView.model.getValue()[0];
        preferences.get('theme').set('positiveColor', newPositiveColor);
        getPreferences(user).savePreferences();
    },
    saveNegativeColorChanges: function(){
        var preferences = getPreferences(user);
        var newNegativeColor = this.negativeColor.currentView.model.getValue()[0];
        preferences.get('theme').set('negativeColor', newNegativeColor);
        getPreferences(user).savePreferences();
    },
    saveWarningColorChanges: function(){
        var preferences = getPreferences(user);
        var newWarningColor = this.warningColor.currentView.model.getValue()[0];
        preferences.get('theme').set('warningColor', newWarningColor);
        getPreferences(user).savePreferences();
    },
    saveFavoriteColorChanges: function(){
        var preferences = getPreferences(user);
        var newFavoriteColor = this.favoriteColor.currentView.model.getValue()[0];
        preferences.get('theme').set('favoriteColor', newFavoriteColor);
        getPreferences(user).savePreferences();
    },
    saveLinksVisitedColorChanges: function(){
        var preferences = getPreferences(user);
        var newLinksVisitedColor = this.linksVisitedColor.currentView.model.getValue()[0];
        preferences.get('theme').set('linksVisitedColor', newLinksVisitedColor);
        getPreferences(user).savePreferences();
    },
    saveLinksColorChanges: function(){
        var preferences = getPreferences(user);
        var newLinksColor = this.linksColor.currentView.model.getValue()[0];
        preferences.get('theme').set('linksColor', newLinksColor);
        getPreferences(user).savePreferences();
    },
    saveChanges: function(){
        this.saveFontChanges();
        this.saveSpacingChanges();
    }
});