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
var wreqr = require('wreqr');
var template = require('./associations-menu.hbs');
var Marionette = require('marionette');
var CustomElements = require('js/CustomElements');
var FilterDropdownView = require('component/dropdown/associations-filter/dropdown.associations-filter.view');
var DisplayDropdownView = require('component/dropdown/associations-display/dropdown.associations-display.view');

//keep around the previously used displayType (not a preference, so only per session)
var displayType = ['list'];
//keep around the previously used displayType (not a preference, so only per session)
var filterType = ['all'];

module.exports = Marionette.LayoutView.extend({
    tagName: CustomElements.register('associations-menu'),
    template: template,
    regions: {
        filterMenu: '> .menu-filter',
        displayMenu: '> .menu-display'
    },
    initialize: function(){

    },
    onBeforeShow: function(){
        this.filterMenu.show(FilterDropdownView.createSimpleDropdown({
            list: [{
                label: 'All associations',
                value: 'all'
            }, {
                label: 'Associated by me',
                value: 'child'
            }, {
                label: 'Associated to me',
                value: 'parent'
            }],
            defaultSelection: filterType
        }));
        this.listenTo(this.getFilterMenuModel(), 'change:value', this.updateFilterType);
        this.displayMenu.show(DisplayDropdownView.createSimpleDropdown({
            list: [{
                label: 'List',
                value: 'list'
            }, {
                label: 'Graph',
                value: 'graph'
            }],
            defaultSelection: displayType
        }));
        this.listenTo(this.getDisplayMenuModel(), 'change:value', this.updateDisplayType);
    },
    updateFilterType: function(){
        filterType = this.getFilterMenuModel().get('value');
    },
    updateDisplayType: function(){
        displayType = this.getDisplayMenuModel().get('value');
    },
    getFilterMenuModel: function(){
        return this.filterMenu.currentView.model;
    },
    getDisplayMenuModel: function(){
        return this.displayMenu.currentView.model;
    }
});