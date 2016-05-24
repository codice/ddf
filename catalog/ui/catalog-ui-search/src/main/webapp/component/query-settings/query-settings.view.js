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
    'component/dropdown/query-sort/dropdown.query-sort.view',
    'component/dropdown/dropdown',
    'component/dropdown/query-federation/dropdown.query-federation.view',
    'component/dropdown/query-src/dropdown.query-src.view',
    'component/property/property.view',
    'component/property/property'
], function (Marionette, _, $, template, CustomElements, store, QuerySortView, DropdownModel,
            QueryFederationView, QuerySrcView, PropertyView, Property) {

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
            this.listenTo(this._federationDropdownModel, 'change:value', this.handleFederationValue);
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
            this._sortDropdownModel = new DropdownModel({
                value: {
                    sortField: this.model.get('sortField'),
                    sortOrder: this.model.get('sortOrder')
                }
            });
            this.settingsSort.show(new QuerySortView({
                model: this._sortDropdownModel
            }));
            this.settingsSort.currentView.turnOffEditing();
        },
        setupFederationDropdown: function(){
            this._federationDropdownModel = new DropdownModel({
                value: this.model.get('federation')
            });
            this.settingsFederation.show(new QueryFederationView({
                model: this._federationDropdownModel
            }));
            this.settingsFederation.currentView.turnOffEditing();
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
            var federation = this._federationDropdownModel.get('value');
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
            var federation = this._federationDropdownModel.get('value');
            this.model.set({
                title: this.settingsTitle.currentView.getCurrentValue()
            });
            this.model.set({
                src: federation === 'selected' ? this._srcDropdownModel.get('value') : undefined
            });
            this.model.set({
                federation: federation
            });
            this.model.set(this._sortDropdownModel.get('value'));
            store.saveQuery();
        }
    });
});
