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
var template = require('./user-settings.hbs');
var CustomElements = require('js/CustomElements');
var ThemeSettings = require('component/theme-settings/theme-settings.view');
var AlertSettings = require('component/alert-settings/alert-settings.view');
var MapSettings = require('component/layers/layers.view');
var SearchSettings = require('component/search-settings/search-settings.view');
var HiddenSettings = require('component/user-blacklist/user-blacklist.view');
var TimeSettings = require('component/time-settings/time-settings.view');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('user-settings'),
    modelEvents: {},
    events: {
        'click > .user-settings-navigation .navigation-choice': 'handleNavigate',
        'click > .user-settings-navigation .choice-theme': 'handleNavigateToTheme',
        'click > .user-settings-navigation .choice-alerts': 'handleNavigateToAlerts',
        'click > .user-settings-navigation .choice-map': 'handleNavigateToMap',
        'click > .user-settings-navigation .choice-search': 'handleNavigateToSearch',
        'click > .user-settings-navigation .choice-hidden': 'handleNavigateToHidden',
        'click > .user-settings-navigation .choice-time': 'handleNavigateToTime',
        'click > .user-settings-content > .content-header .header-back': 'handleBack'
    },
    regions: {
        settingsContent: '> .user-settings-content > .content-settings'
    },
    ui: {},
    onBeforeShow: function() {
    },
    handleBack: function(){
        this.$el.toggleClass('is-navigated', false);
        this.settingsContent.empty();
        this.repositionDropdown();
    },
    handleNavigate: function(){
        this.$el.toggleClass('is-navigated', true);
    },
    handleNavigateToSearch: function(){
        this.settingsContent.show(new SearchSettings());
    },
    handleNavigateToMap: function(){
        this.settingsContent.show(new MapSettings());
    },
    handleNavigateToAlerts: function(){
        this.settingsContent.show(new AlertSettings());
        this.repositionDropdown();
    },
    handleNavigateToTime: function(){
        this.settingsContent.show(new TimeSettings());
    },
    handleNavigateToTheme: function(){
        //this.$el.find('> .user-settings-content > .content-header .header-title').html('Theme');
        this.settingsContent.show(new ThemeSettings());
        this.repositionDropdown();
    },
    handleNavigateToHidden: function(){
        this.settingsContent.show(new HiddenSettings());
    },
    repositionDropdown: function(){
        this.$el.trigger('repositionDropdown.'+CustomElements.getNamespace());
    }
});