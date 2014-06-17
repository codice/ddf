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
                leftRegion: {
                    selector: "#searchPages",
                    regionType:  SlidingRegion
                }
            },

            initialize: function (options) {
                _.bindAll(this);

                this.queryForm = new QueryFormView({
                    sources : options.sources
                });

                wreqr.vent.on('query:update', this.updateScrollbar);
                wreqr.vent.on('search:show', this.showQuery);
                wreqr.vent.on('search:clear', this.onQueryClear, this);
                wreqr.vent.on('search:start', this.onQueryClear, this);
                wreqr.vent.on('search:start', this.setupProgress, this);
                wreqr.vent.on('search:start', this.showEmptyResults, this);
                wreqr.vent.on('search:results', this.showResults, this);
                wreqr.vent.on('metacard:selected', this.showMetacardDetail, this);

                wreqr.vent.on('searchcontrol:back', this.back, this);
                wreqr.vent.on('searchcontrol:forward', this.forward, this);

                this.modelBinder = new Backbone.ModelBinder();
            },

            updateScrollbar: function () {
                var view = this;
                // defer seems to be necessary for this to update correctly
                _.defer(function () {
                    view.leftRegion.$el.perfectScrollbar('update');
                });
            },

            onRender : function(){
                this.leftRegion.show(this.queryForm, dir.forward);
                this.searchControlRegion.show(new SearchControl.SearchControlView());

                if(maptype.isNone()) {
                    this.$el.addClass('full-screen-search');
                }
            },
            setupProgress: function (resultList, queryModel, numSources, progressObj) {
                if (!resultList.useAjaxSync) {
                    if (numSources > 1) {
                        if (this.progressView) {
                            this.progressView.close();
                        }
                        this.progressView = new ProgressView({ resultList: resultList, queryModel: queryModel, sources: numSources, model: progressObj});
                        this.progressRegion.show(this.progressView);
                    }
                }
            },
            onQueryClear: function () {
                if (this.metacardDetail) {
                    this.metacardDetail.remove();
                    delete this.metacardDetail;
                }
                if (this.progressView) {
                    this.progressView.close();
                }
                if (this.resultList) {
                    this.resultList.close();
                    delete this.resultList;
                }
            },
            back: function () {
                if (this.leftRegion.currentView === this.resultList) {
                    //go back to query
                    this.showQuery(dir.backward);
                }
                else if (this.leftRegion.currentView === this.metacardDetail) {
                    this.showResults(null, dir.backward);
                }
            },
            forward: function () {
                if (this.leftRegion.currentView === this.queryForm) {
                    this.showEmptyResults();
                }
                else if (this.leftRegion.currentView === this.resultList) {
                    this.showMetacardDetail(null, dir.forward);
                }
            },
            showQuery: function (direction) {
                this.leftRegion.show(this.queryForm, direction);
            },
            showEmptyResults: function() {
                this.showResults(null, dir.forward);
            },
            showResults: function (result, direction) {
                if (result) {
                    this.resultList = new MetacardList.MetacardListView({model: result});
                }
                if(!this.resultList){
                    this.resultList = new MetacardList.MetacardListView({model: new MetacardModel.SearchResult()});
                }
                this.leftRegion.show(this.resultList, direction);
            },
            showMetacardDetail: function (metacard, direction) {
                if (!metacard && this.metacardDetail) {
                    this.metacardDetail.model.set('direction', dir.forward);
                    metacard = this.metacardDetail.model;
                }

                if (metacard) {
                    if (this.metacardDetail) {
                        this.stopListening(this.metacardDetail, 'content-update', this.updateScrollbar);
                    }
                    this.metacardDetail = new MetacardDetail.MetacardDetailView({metacard: metacard});
                    this.listenTo(this.metacardDetail, 'content-update', this.updateScrollbar);
                    direction = _.isUndefined(metacard.get('direction')) ? direction : metacard.get('direction');
                }

                this.leftRegion.show(this.metacardDetail, direction);
            }
        });

        return Search;
    });
