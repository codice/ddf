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
/*global define, setTimeout*/
define([
    'marionette',
    'underscore',
    'jquery',
    'text!./alert-settings.hbs',
    'js/CustomElements',
    'js/model/user',
    'component/property/property.view',
    'component/property/property',
    'component/dropdown/dropdown.view',
    'component/radio/radio.view'
], function (Marionette, _, $, template, CustomElements, user, PropertyView, Property,
             DropdownView, RadioView) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('alert-settings'),
        modelEvents: {},
        events: {
            'click .editor-edit': 'turnOnEditing',
            'click .editor-cancel': 'cancel',
            'click .editor-save': 'save'
        },
        regions: {
            propertyPersistance: '.property-persistance .property-region',
            propertyExpiration: '.property-expiration .property-region'
        },
        ui: {},
        onBeforeShow: function () {
            this.setupPersistance();
            this.setupExpiration();
            this.turnOffEditing();
            this.handlePeristance();
            this.startListeningToRegions();
        },
        startListeningToRegions: function(){
            this.listenTo(this.propertyPersistance.currentView.model, 'change:value', this.handlePeristance);
        },
        stopListeningToRegions: function(){
            this.stopListening(this.propertyPersistance.currentView.model);
        },
        handlePeristance: function () {
            var persistance = this.propertyPersistance.currentView.model.get('value');
            this.$el.toggleClass('is-persisted', persistance);
        },
        setupPersistance: function () {
            var persistance = user.get('user').get('preferences').get('alertPersistance');
            this.propertyPersistance.show(RadioView.createRadio([{
                label: 'Yes',
                value: 'true'
            }, {
                label: 'No',
                value: 'false'
            }], persistance));
        },
        setupExpiration: function () {
            var expiration = user.get('user').get('preferences').get('alertExpiration');
            var millisecondsInDay = 24 * 60 * 60 * 1000;
            this.propertyExpiration.show(DropdownView.createSimpleDropdown([
                {
                    label: '1 Day',
                    value: 1 * millisecondsInDay
                }, {
                    label: '2 Days',
                    value: 2 * millisecondsInDay
                }, {
                    label: '4 Days',
                    value: 4 * millisecondsInDay
                }, {
                    label: '1 Week',
                    value: 7 * millisecondsInDay
                }, {
                    label: '2 Weeks',
                    value: 14 * millisecondsInDay
                }, {
                    label: '1 Month',
                    value: 30 * millisecondsInDay
                }, {
                    label: '2 Months',
                    value: 60 * millisecondsInDay
                }, {
                    label: '4 Months',
                    value: 120 * millisecondsInDay
                }, {
                    label: '6 Months',
                    value: 180 * millisecondsInDay
                }, {
                    label: '1 Year',
                    value: 365 * millisecondsInDay
                }
            ], false, [expiration]));
        },
        turnOnEditing: function () {
            this.$el.addClass('is-editing');
            this.regionManager.forEach(function (region) {
                if (region.currentView) {
                    region.currentView.turnOnEditing();
                }
            });
        },
        turnOffEditing: function () {
            this.regionManager.forEach(function (region) {
                if (region.currentView) {
                    region.currentView.turnOffEditing();
                }
            });
        },
        cancel: function () {
            this.$el.removeClass('is-editing');
            this.stopListeningToRegions();
            this.onBeforeShow();
        },
        save: function () {
            this.$el.removeClass('is-editing');
            var preferences = user.get('user').get('preferences');
            preferences.set({
                alertPersistance: this.propertyPersistance.currentView.model.get('value'),
                alertExpiration: this.propertyExpiration.currentView.model.get('value')[0]
            });
            preferences.savePreferences();
            this.$el.trigger(CustomElements.getNamespace() + 'close-lightbox');
        }
    });
});
