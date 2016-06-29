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
    'text!./workspaces-items.hbs',
    'js/CustomElements',
    'js/store',
    'component/workspace-item/workspace-item.collection.view',
    'component/dropdown/dropdown',
    'component/dropdown/workspaces-filter/dropdown.workspaces-filter.view',
    'component/dropdown/workspaces-sort/dropdown.workspaces-sort.view',
    'component/dropdown/workspaces-display/dropdown.workspaces-display.view',
    'js/model/user'
], function (wreqr, Marionette, _, $, template, CustomElements, store, WorkspaceItemCollection, DropdownModel, FilterDropdownView,
        SortDropdownView, DisplayDropdownView, user) {

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
        },
        onBeforeShow: function(){
            var preferences = user.get('user').get('preferences');

            var workspaceItemCollection = new WorkspaceItemCollection({
                collection: this.model
            });

            var displayDropdownModel = new DropdownModel({value: preferences.get('homeDisplay')});
            var homeFilter = new DropdownModel({value: preferences.get('homeFilter')});
            var homeSort = new DropdownModel({value: preferences.get('homeSort')});

            this.listenTo(displayDropdownModel, 'change:value', this.save('homeDisplay'));
            this.listenTo(homeFilter, 'change:value', this.save('homeFilter'));
            this.listenTo(homeSort, 'change:value', this.save('homeSort'));

            this.homeItems.show(workspaceItemCollection);

            this.homeFilter.show(new FilterDropdownView({
                model: homeFilter
            }));
            this.homeSort.show(new SortDropdownView({
                model: homeSort
            }));
            this.homeDisplay.show(new DisplayDropdownView({
                model: displayDropdownModel
            }));
        },
        save: function (key) {
            return function (model, value) {
                var prefs = user.get('user').get('preferences');
                prefs.set(key, value);
                prefs.savePreferences();
            }.bind(this);
        }
    });
});
