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
    './query-settings.hbs',
    'js/CustomElements',
    'js/store',
    'component/dropdown/dropdown',
    'component/dropdown/query-src/dropdown.query-src.view',
    'component/property/property.view',
    'component/property/property'
], function (Marionette, _, $, template, CustomElements, store, DropdownModel,
            QuerySrcView, PropertyView, Property) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('query-settings'),
        modelEvents: {
        },
        events: {
            'click .editor-edit': 'turnOnEditing',
            'click .editor-cancel': 'cancel',
            'click .editor-save': 'save'
        },
        regions: {
            settingsSort: '.settings-sorting',
            settingsFederation: '.settings-federation',
            settingsSrc: '.settings-src'
        },
        ui: {
        },
        focus: function(){
            this.regionManager.first().currentView.focus();
        },
        onBeforeShow: function(){
            this.setupSortDropdown();
            this.setupFederationDropdown();
            this.setupSrcDropdown();
            this.listenTo(this.settingsFederation.currentView.model, 'change:value', this.handleFederationValue);
            this.handleFederationValue();
            if (this.model._cloneOf === undefined){
                this.turnOnEditing();
            }
        },
        setupSortDropdown: function(){
            var defaultValue = {
                sortField: this.model.get('sortField')
            };
            if (defaultValue.sortField !== 'RELEVANCE') {
                defaultValue.sortOrder = this.model.get('sortOrder');
            }
            this.settingsSort.show(new PropertyView({
                model: new Property({
                    enum: [
                        {
                            label: 'Best Text Match',
                            value: {
                                sortField: 'RELEVANCE'
                            }
                        },
                        {
                            label: 'Shortest Distance',
                            value: {
                                sortField: 'DISTANCE',
                                sortOrder: 'asc'
                            }
                        },
                        {
                            label: 'Furthest Distance',
                            value: {
                                sortField: 'DISTANCE',
                                sortOrder: 'desc'
                            }
                        },
                        {
                            label: 'Earliest Modified',
                            value: {
                                sortField: 'modified',
                                sortOrder: 'asc'
                            }
                        },
                        {
                            label: 'Latest Modified',
                            value: {
                                sortField: 'modified',
                                sortOrder: 'desc'
                            }
                        },
                        {
                            label: 'Earliest Created',
                            value: {
                                sortField: 'created',
                                sortOrder: 'asc'
                            }
                        },
                        {
                            label: 'Latest Created',
                            value: {
                                sortField: 'created',
                                sortOrder: 'desc'
                            }
                        },
                        {
                            label: 'Earliest Effective',
                            value: {
                                sortField: 'effective',
                                sortOrder: 'asc'
                            }
                        },
                        {
                            label: 'Latest Effective',
                            value: {
                                sortField: 'effective',
                                sortOrder: 'desc'
                            }
                        },
                    ],
                    value: [defaultValue],
                    id: 'Sorting'
                })
            }));
            this.settingsSort.currentView.turnOffEditing();
            this.settingsSort.currentView.turnOnLimitedWidth();
        },
        setupFederationDropdown: function(){
            this.settingsFederation.show(new PropertyView({
                model: new Property({
                    enum: [
                        {
                            label: 'All Sources',
                            value: 'enterprise'
                        },
                        {
                            label: 'Specific Sources',
                            value: 'selected'
                        },
                        {
                            label: 'None',
                            value: 'local'
                        }
                    ],
                    value: [this.model.get('federation')],
                    id: 'Federation'
                })
            }));
            this.settingsFederation.currentView.turnOffEditing();
            this.settingsFederation.currentView.turnOnLimitedWidth();
        },
        setupSrcDropdown: function(){
            var sources = this.model.get('src');
            this._srcDropdownModel = new DropdownModel({
                value: sources ? sources : []
            });
            this.settingsSrc.show(new QuerySrcView({
                model: this._srcDropdownModel
            }));
            this.settingsSrc.currentView.turnOffEditing();
        },
        handleFederationValue: function(){
            var federation = this.settingsFederation.currentView.model.getValue()[0];
            this.$el.toggleClass('is-specific-sources', federation === 'selected');
        },
        turnOnEditing: function(){
           this.$el.addClass('is-editing');
            this.regionManager.forEach(function(region){
                if (region.currentView && region.currentView.turnOnEditing){
                    region.currentView.turnOnEditing();
                }
            });
            this.focus();
        },
        cancel: function(){
            if (this.model._cloneOf === undefined){
                store.resetQuery();
            } else {
                this.$el.removeClass('is-editing');
                this.onBeforeShow();
            }
        },
        saveToModel: function(){
            var federation = this.settingsFederation.currentView.model.getValue()[0];
            this.model.set({
                src: federation === 'selected' ? this._srcDropdownModel.get('value') : undefined
            });
            this.model.set({
                federation: federation
            });
            this.model.set(this.settingsSort.currentView.model.getValue()[0]);
        },
        save: function(){
            this.$el.removeClass('is-editing');
            this.saveToModel();
            store.saveQuery();
        }
    });
});
