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
    './query-schedule.hbs',
    'js/CustomElements',
    'js/store',
    'properties',
    'component/property/property.view',
    'component/property/property',
    'component/dropdown/dropdown.view',
    'moment',
    'js/Common'
], function(Marionette, _, $, template, CustomElements, store, properties, PropertyView, Property,
    DropdownView, Moment, Common) {

    function getHumanReadableDuration(milliseconds) {
        var duration = Moment.duration(milliseconds);
        var days = duration.days();
        var hours = duration.hours();
        var minutes = duration.minutes();
        var seconds = duration.seconds();
        var result = days ? (days + ' day(s) ') : '';
        result += hours ? (hours + ' hour(s) ') : '';
        result += minutes ? (minutes + ' minute(s) ') : '';
        result += seconds ? (seconds + ' second(s)') : '';
        return result.trim();
    }

    var pollingFrequencyEnum = properties.scheduleFrequencyList.sort(function(a, b) {
        return a - b;
    }).reduce(function(options, option) {
        var durationInMilliseconds = option * 1000;
        options.push({
            label: getHumanReadableDuration(durationInMilliseconds),
            value: durationInMilliseconds
        });
        return options;
    }, [{
        label: 'Never',
        value: false
    }]);

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('query-schedule'),
        modelEvents: {},
        events: {
            'click .editor-edit': 'turnOnEditing',
            'click .editor-cancel': 'cancel',
            'click .editor-save': 'save'
        },
        regions: {
            propertyInterval: '.property-interval'
        },
        ui: {},
        initialize: function(){
            this.model = this.model._cloneOf ? store.getQueryById(this.model._cloneOf) : this.model;
            this.listenTo(this.model, 'change:polling', Common.safeCallback(this.onBeforeShow));
        },
        onBeforeShow: function() {
            this.setupInterval();
            this.turnOnEditing();
        },
        setupInterval: function() {
            this.propertyInterval.show(new PropertyView({
                model: new Property({
                    enum: pollingFrequencyEnum,
                    value: [this.model.get('polling') || false],
                    id: 'Frequency'
                })
            }));
            this.propertyInterval.currentView.turnOffEditing();
        },
        turnOnEditing: function() {
            this.$el.addClass('is-editing');
            this.regionManager.forEach(function(region) {
                if (region.currentView) {
                    region.currentView.turnOnEditing();
                }
            });
        },
        turnOffEditing: function() {
            this.regionManager.forEach(function(region) {
                if (region.currentView) {
                    region.currentView.turnOffEditing();
                }
            });
        },
        cancel: function() {
            this.$el.removeClass('is-editing');
            this.onBeforeShow();
            this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
        },
        save: function() {
            var value =  this.propertyInterval.currentView.model.getValue()[0];
            if (value === false) {
                this.model.unset('polling');
            } else {
                this.model.set({
                    polling: value
                });
            }
            this.cancel();
        }
    });
});