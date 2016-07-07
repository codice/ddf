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
    'text!./query-settings.hbs',
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
            'click .editor-edit': 'edit',
            'click .editor-cancel': 'cancel',
            'click .editor-save': 'save'
        },
        regions: {
            settingsTitle: '.settings-title',
            settingsSort: '.settings-sorting',
            settingsFederation: '.settings-federation',
            settingsSrc: '.settings-src'
        },
        ui: {
        },
        onBeforeShow: function(){
            this.setupSortDropdown();
            this.setupFederationDropdown();
            this.setupSrcDropdown();
            this.setupTitleInput();
            this.settingsFederation.currentView.$el.on('change', this.handleFederationValue.bind(this));
            this.handleFederationValue();
            if (this.model._cloneOf === undefined){
                this.edit();
            }
        },
        setupTitleInput: function(){
            this.settingsTitle.show(new PropertyView({
                model: new Property({
                    value: [this.model.get('title')],
                    id: 'Name'
                })
            }));
            this.settingsTitle.currentView.turnOffEditing();
            this.settingsTitle.currentView.turnOnLimitedWidth();
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
            var federation = this.settingsFederation.currentView.getCurrentValue()[0];
            this.$el.toggleClass('is-specific-sources', federation === 'selected');
        },
        edit: function(){
            this.$el.addClass('is-editing');
            this.settingsSort.currentView.turnOnEditing();
            this.settingsFederation.currentView.turnOnEditing();
            this.settingsSrc.currentView.turnOnEditing();
            this.settingsTitle.currentView.turnOnEditing();
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
            var federation = this.settingsFederation.currentView.getCurrentValue()[0];
            this.model.set({
                title: this.settingsTitle.currentView.getCurrentValue()
            });
            this.model.set({
                src: federation === 'selected' ? this._srcDropdownModel.get('value') : undefined
            });
            this.model.set({
                federation: federation
            });
            this.model.set(this.settingsSort.currentView.getCurrentValue()[0]);
            store.saveQuery();
        }
    });
});
