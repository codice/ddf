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
        'icanhaz',
        'wreqr',
        'text!templates/search.panel.handlebars',
        'js/view/SearchControl.view',
        'maptype',
        // Load non attached libs and plugins
        'perfectscrollbar'
    ],
    function ($, _, Marionette, SlidingRegion, QueryView, Progress, MetacardList, MetacardDetail, MetacardModel, Backbone, dir, ich, wreqr, searchPanel, SearchControl, maptype) {
        "use strict";
        var QueryFormView = QueryView.QueryView,
            ProgressView = Progress.ProgressView,
            Search = {};

        ich.addTemplate('searchPanel', searchPanel);

        Search.SearchLayout = Marionette.Layout.extend({
            template : 'searchPanel',
            className: 'partialaffix span3 row-fluid nav nav-list well well-small search-controls',
            regions : {
                progressRegion: "#progressRegion",
                searchControlRegion: "#searchControlRegion",
                searchRegion: {
                    selector: "#searchPages",
                    regionType:  SlidingRegion
                }
            },

            initialize: function (options) {
                _.bindAll(this);

                this.queryForm = new QueryFormView({
                    sources : options.sources
                });

                wreqr.vent.on('search:show', this.showQuery);
                wreqr.vent.on('search:clear', this.onQueryClear);
                wreqr.vent.on('search:start', this.onQueryClear);
                wreqr.vent.on('search:start', this.setupProgress);
                wreqr.vent.on('search:start', this.showEmptyResults);
                wreqr.vent.on('search:results', this.showResults);
                wreqr.vent.on('metacard:selected', this.showMetacardDetail);
                wreqr.vent.on('search:back', this.back);
                wreqr.vent.on('search:forward', this.forward);
            },

            onRender : function(){
                this.searchRegion.show(this.queryForm, dir.forward);
                this.searchControlRegion.show(new SearchControl.SearchControlView());

                if(maptype.isNone()) {
                    this.$el.addClass('full-screen-search');
                }
            },
            setupProgress: function (resultList, queryModel, numSources, progressObj) {
                if (!resultList.useAjaxSync) {
                    if (numSources > 1) {
                        this.progressRegion.show(new ProgressView({ resultList: resultList, queryModel: queryModel, sources: numSources, model: progressObj}));
                    }
                }
            },
            onQueryClear: function () {
                if (this.progressRegion.currentView) {
                    this.progressRegion.close();
                }
                delete this.result;
                delete this.metacard;
            },
            back: function (currentState) {
                if (currentState === 'results') {
                    //go back to query
                    this.showQuery(dir.backward);
                }
                else if (currentState === 'record') {
                    this.showResults(null, dir.backward);
                }
            },
            forward: function (currentState) {
                if (currentState === 'search') {
                    this.showEmptyResults();
                }
                else if (currentState === 'results') {
                    this.showMetacardDetail(null, dir.forward);
                }
            },
            showQuery: function (direction) {
                this.searchRegion.show(this.queryForm, direction);
            },
            showEmptyResults: function() {
                this.showResults(null, dir.forward);
            },
            showResults: function (result, direction) {
                if (result) {
                    this.result = result;
                    this.searchRegion.show(new MetacardList.MetacardListView({model: result}), direction);
                } else {
                    if(!this.result) {
                        this.result = new MetacardModel.SearchResult();
                    }
                    this.searchRegion.show(new MetacardList.MetacardListView({model: this.result}), direction);
                }
            },
            showMetacardDetail: function (metacard, direction) {
                if (!metacard && this.metacard) {
                    this.metacard.set('direction', dir.forward);
                    metacard = this.metacard;
                }

                if (metacard) {
                    this.metacard = metacard;
                    direction = _.isUndefined(metacard.get('direction')) ? direction : metacard.get('direction');
                    this.searchRegion.show(new MetacardDetail.MetacardDetailView({model: metacard}), direction);
                }
            }
        });

        return Search;
    });
