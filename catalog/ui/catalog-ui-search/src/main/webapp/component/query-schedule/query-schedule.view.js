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
    'text!./query-schedule.hbs',
    'js/CustomElements',
    'js/store',
    'component/property/property.view',
    'component/property/property',
    'component/dropdown/dropdown.view',
    'component/radio/radio.view'
], function (Marionette, _, $, template, CustomElements, store, PropertyView, Property,
             DropdownView, RadioView) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('query-schedule'),
        modelEvents: {
        },
        events: {
            'click .editor-edit': 'turnOnEditing',
            'click .editor-cancel': 'cancel',
            'click .editor-save': 'save'
        },
        regions: {
            propertyInterval: '.property-interval'
        },
        ui: {
        },
        onBeforeShow: function(){
            this.setupInterval();
            if (this.model._cloneOf === undefined){
                this.turnOnEditing();
            } else {
                this.turnOffEditing();
            }
        },
        setupInterval: function(){
            var halfHour = 30 * 60 * 1000;
            var hour = 2 * halfHour;
            this.propertyInterval.show(new PropertyView({
                model: new Property({
                    enum: [
                        {
                            label: 'Never',
                            value: false
                        },
                        {
                            label: '1/2 Hour',
                            value: halfHour
                        },
                        {
                            label: '1 Hour',
                            value: hour
                        },
                        {
                            label: '2 Hours',
                            value: 2 * hour
                        },
                        {
                            label: '4 Hours',
                            value: 4 * hour
                        },
                        {
                            label: '8 Hours',
                            value: 8 * hour
                        },
                        {
                            label: '16 Hours',
                            value: 16 * hour
                        },
                        {
                            label: 'Day',
                            value: 24 * hour
                        }
                    ],
                    value: [this.model.get('polling') || false],
                    id: 'Frequency'
                })
            }));
            this.propertyInterval.currentView.turnOffEditing();
            this.propertyInterval.currentView.turnOnLimitedWidth();
        },
        turnOnEditing: function(){
            this.$el.addClass('is-editing');
            this.regionManager.forEach(function(region){
                if (region.currentView){
                    region.currentView.turnOnEditing();
                }
            });
        },
        turnOffEditing: function(){
            this.regionManager.forEach(function(region){
                if (region.currentView){
                    region.currentView.turnOffEditing();
                }
            });
        },
        cancel: function(){
            if (this.model._cloneOf === undefined){
                store.resetQuery();
            } else {
                this.$el.removeClass('is-editing');
                this.onBeforeShow();
            }
        },
        save: function(){
            this.$el.removeClass('is-editing');
            this.model.set({
                polling: this.propertyInterval.currentView.getCurrentValue()[0]
            });
            store.saveQuery();
        }
    });
});
