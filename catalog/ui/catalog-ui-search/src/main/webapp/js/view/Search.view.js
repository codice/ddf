/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/
define([
    'jquery',
    'underscore',
    'marionette',
    'js/view/sliding.region',
    'js/view/Query.view',
    'js/view/Progress.view',
    'js/view/MetacardList.view',
    'js/view/MetacardDetail.view',
    'js/model/Metacard',
    'backbone',
    'direction',
    'wreqr',
    'text!templates/search/search.panel.handlebars',
    'js/view/SearchControl.view',
    'js/model/Query',
    'js/view/WorkspaceSaveResults.view',
    'js/controllers/Filter.controller',
    'js/store',
    // Load non attached libs and plugins
    'perfectscrollbar'
], function ($, _, Marionette, SlidingRegion, QueryView, Progress, MetacardList, MetacardDetail, MetacardModel, Backbone, dir, wreqr, searchPanel, SearchControl, QueryModel, WorkspaceSaveResults, FilterController, store) {
    'use strict';
    var Search = {};
    Search.SearchLayout = Marionette.LayoutView.extend({
        template: searchPanel,
        className: 'height-full',
        regions: {
            progressRegion: '#progressRegion',
            searchControlRegion: '#searchControlRegion',
            searchRegion: {
                selector: '#searchPages',
                regionClass: SlidingRegion
            }
        },
        initialize: function () {
            _.bindAll(this);
            this.query = new QueryModel.Model();
            this.controller = new FilterController();
            this.listenTo(wreqr.vent, 'workspace:tabshown', this.setupWreqr);
        },
        setupWreqr: function (tabHash) {
            if (this.result && tabHash === '#search') {
                wreqr.vent.trigger('map:clear');
                wreqr.vent.trigger('map:results', this.result, false);
                if (this.query) {
                    this.updateMapPrimitive();
                }
            }
            if (tabHash === '#search') {
                this.listenTo(wreqr.vent, 'search:show', this.showQuery);
                this.listenTo(wreqr.vent, 'search:clear', this.onQueryClear);
                this.listenTo(wreqr.vent, 'search:start', this.onQueryClear);
                this.listenTo(wreqr.vent, 'search:start', this.setupProgress);
                this.listenTo(wreqr.vent, 'search:start', this.showEmptyResults);
                this.listenTo(wreqr.vent, 'search:results', this.showResults);
                this.listenTo(wreqr.vent, 'metacard:selected', this.showMetacardDetail);
                this.listenTo(wreqr.vent, 'workspace:saveresults', this.saveResultsToWorkspace);
                this.listenTo(wreqr.vent, 'workspace:resultssavecancel', this.cancelResultsToWorkspace);
                this.listenTo(wreqr.vent, 'workspace:searchsavecancel', this.cancelSearchToWorkspace);
                this.searchRegion.show(undefined, dir.none);
            } else {
                this.stopListening(wreqr.vent, 'search:show');
                this.stopListening(wreqr.vent, 'search:clear');
                this.stopListening(wreqr.vent, 'search:start');
                this.stopListening(wreqr.vent, 'search:start');
                this.stopListening(wreqr.vent, 'search:start');
                this.stopListening(wreqr.vent, 'search:results');
                this.stopListening(wreqr.vent, 'metacard:selected');
                this.stopListening(wreqr.vent, 'workspace:saveresults');
                this.stopListening(wreqr.vent, 'workspace:resultssavecancel');
                this.stopListening(wreqr.vent, 'workspace:searchsavecancel');
                this.searchRegion.destroy();
            }
        },
        saveResultsToWorkspace: function (search, records) {
            var workspaces = store.get('workspaces');
            this.searchRegion.show(new WorkspaceSaveResults({
                model: workspaces,
                search: search,
                records: records
            }), dir.forward);
        },
        cancelResultsToWorkspace: function () {
            this.showResults(dir.backward);
        },
        cancelSearchToWorkspace: function () {
            this.showQuery(dir.backward);
        },
        updateMapPrimitive: function () {
            wreqr.vent.trigger('search:drawend');
            if (this.query.get('north') && this.query.get('south') && this.query.get('east') && this.query.get('west')) {
                wreqr.vent.trigger('search:bboxdisplay', this.query);
                this.query.trigger('EndExtent');
            } else if (this.query.get('lat') && this.query.get('lon') && this.query.get('radius')) {
                wreqr.vent.trigger('search:circledisplay', this.query);
                this.query.trigger('EndExtent');
            }
        },
        onRender: function () {
            this.searchRegion.show(new QueryView.QueryView({ model: this.query }), dir.forward);
            this.searchControlRegion.show(new SearchControl.SearchControlView());
        },
        setupProgress: function (queryModel, progressObj) {
            this.progressRegion.show(new Progress.ProgressView({
                resultList: queryModel.get('result'),
                queryModel: queryModel,
                model: progressObj
            }));
        },
        onQueryClear: function () {
            if (this.progressRegion.currentView) {
                this.progressRegion.destroy();
            }
            delete this.result;
        },
        showQuery: function (direction, query) {
            if (query) {
                this.query = query;
            }
            this.searchRegion.show(new QueryView.QueryView({ model: this.query }), direction);
        },
        showEmptyResults: function () {
            this.showResults(dir.forward);
        },
        showResults: function (direction, result) {
            if (result) {
                this.result = result;
                this.searchRegion.show(new MetacardList.MetacardListView({ model: this.result }), direction);
            } else {
                if (!this.result) {
                    this.result = new MetacardModel.SearchResult();
                }
                this.searchRegion.show(new MetacardList.MetacardListView({ model: this.result }), direction);
            }
        },
        showMetacardDetail: function (direction, metacard) {
            this.searchRegion.show(new MetacardDetail.MetacardDetailView({ model: metacard }), direction);
        }
    });
    return Search;
});
