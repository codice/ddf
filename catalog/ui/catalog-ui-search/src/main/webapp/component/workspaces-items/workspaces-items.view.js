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
/*global define*/
define([
    'wreqr',
    'marionette',
    'underscore',
    'jquery',
    './workspaces-items.hbs',
    'js/CustomElements',
    'js/store',
    'component/workspace-item/workspace-item.collection.view',
    'component/dropdown/dropdown',
    'component/dropdown/workspaces-filter/dropdown.workspaces-filter.view',
    'component/dropdown/workspaces-sort/dropdown.workspaces-sort.view',
    'component/dropdown/workspaces-display/dropdown.workspaces-display.view',
    'component/singletons/user-instance'
], function (wreqr, Marionette, _, $, template, CustomElements, store, WorkspaceItemCollection, DropdownModel, FilterDropdownView,
        SortDropdownView, DisplayDropdownView, user) {

    var getUser = function () {
        return user.get('user');
    };

    var getPrefs = function () {
         return getUser().get('preferences');
    };

    return Marionette.LayoutView.extend({
        setDefaultModel: function(){
            this.model = store.get('workspaces');
        },
        template: template,
        tagName: CustomElements.register('workspaces-items'),
        modelEvents: {
        },
        regions: {
            homeFilter: '.home-items-filter',
            homeSort: '.home-items-sort',
            homeDisplay: '.home-items-display',
            homeItems: '.home-items-choices'
        },
        initialize: function(options){
            if (!options.model){
                this.setDefaultModel();
            }
            this.handleItems();
            this.handleSort();
            this.listenTo(getPrefs(), 'change:homeSort', this.handleSort);
            this.listenTo(this.model, 'add remove', this.handleItems);
        },
        handleItems: function(){
            this.$el.toggleClass('is-empty', this.model.length === 0);
        },
        handleSort: function(){
            this.$el.toggleClass('by-title', getPrefs().get('homeSort') === 'Title');
            this.$el.toggleClass('by-date', getPrefs().get('homeSort') === 'Last modified');
        },
        onBeforeShow: function(){
            this.setupHomeItems();
            this.setupHomeFilter();
            this.setupHomeSort();
            this.setupHomeDisplay();
        },
        setupHomeItems: function(){
            var workspaceItemCollection = new WorkspaceItemCollection({
                collection: this.model
            });
             this.homeItems.show(workspaceItemCollection);
        },
        setupHomeFilter: function(){
            var preferences = user.get('user').get('preferences');
            this.homeFilter.show(FilterDropdownView.createSimpleDropdown(
                {
                    list: [
                        {
                            label: 'Owned by anyone',
                            value: 'Owned by anyone',
                        },
                        {
                            label: 'Owned by me',
                            value: 'Owned by me',
                        },
                        {
                            label: 'Not owned by me',
                            value: 'Not owned by me',
                        }
                    ],
                    defaultSelection: [preferences.get('homeFilter')]
                }
            ));
            this.listenTo(this.homeFilter.currentView.model, 'change:value', this.save('homeFilter'));
        },
        setupHomeDisplay: function(){
            var preferences = user.get('user').get('preferences');
            this.homeDisplay.show(DisplayDropdownView.createSimpleDropdown(
                {
                    list: [
                        {
                            label: 'Grid',
                            value: 'Grid',
                        },
                        {
                            label: 'List',
                            value: 'List',
                        }
                    ],
                    defaultSelection: [preferences.get('homeDisplay')]
                }
            ));
            this.listenTo(this.homeDisplay.currentView.model, 'change:value', this.save('homeDisplay'));
        },
        setupHomeSort: function(){
            var preferences = user.get('user').get('preferences');
            this.homeSort.show(SortDropdownView.createSimpleDropdown(
                {
                    list: [
                        {
                            label: 'Last modified',
                            value: 'Last modified',
                        },
                        {
                            label: 'Title',
                            value: 'Title',
                        }
                    ],
                    defaultSelection: [preferences.get('homeSort')]
                }
            ));
            this.listenTo(this.homeSort.currentView.model, 'change:value', this.save('homeSort'));
        },
        save: function (key) {
            return function (model, value) {
                var prefs = user.get('user').get('preferences');
                prefs.set(key, value[0]);
                prefs.savePreferences();
            }.bind(this);
        }
    });
});
