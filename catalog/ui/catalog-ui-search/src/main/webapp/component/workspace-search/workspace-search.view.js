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
var Marionette = require('marionette');
var CustomElements = require('js/CustomElements');
var template = require('./workspace-search.hbs');
var ResultsView = require('component/results/results.view');
var SearchesView = require('component/workspace-explore/workspace-explore.view');
var store = require('js/store');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('workspace-search'),
    regions: {
        searchAdd: '> .search-add',
        searchResults: '> .search-results'
    },
    onBeforeShow: function () {
        if (store.getCurrentWorkspace()) {
            this.setupSearchAdd();
            this.setupSearchResults();
        }
    },
    setupSearchAdd: function () {
       //this.searchAdd.show(new SearchesView());
    },
    setupSearchResults: function(){
        this.searchResults.show(new ResultsView());
    }
});