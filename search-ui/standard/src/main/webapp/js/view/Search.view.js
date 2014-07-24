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
        'text!templates/search/search.panel.handlebars',
        'js/view/SearchControl.view',
        'js/model/Query',
        // Load non attached libs and plugins
        'perfectscrollbar'
    ],
    function ($, _, Marionette, SlidingRegion, QueryView, Progress, MetacardList, MetacardDetail, MetacardModel, Backbone, dir, ich, wreqr, searchPanel, SearchControl, QueryModel) {
        "use strict";
        var Search = {};

        ich.addTemplate('searchPanel', searchPanel);

        Search.SearchLayout = Marionette.Layout.extend({
            template : 'searchPanel',
            className: 'height-full',
            regions : {
                progressRegion: "#progressRegion",
                searchControlRegion: "#searchControlRegion",
                searchRegion: {
                    selector: "#searchPages",
                    regionType:  SlidingRegion
                }
            },

            initialize: function () {
                _.bindAll(this);

                this.query = new QueryModel.Model();

                wreqr.vent.on('workspace:tabshown', _.bind(this.setupWreqr, this));
            },

            setupWreqr: function(tabHash) {
                if(this.result && tabHash === '#search') {
                    wreqr.vent.trigger('map:clear');
                    wreqr.vent.trigger('map:results', this.result, false);
                    if(this.query) {
                        this.updateMapPrimitive();
                    }
                }

                if(tabHash === '#search') {
                    wreqr.vent.on('search:show', this.showQuery);
                    wreqr.vent.on('search:clear', this.onQueryClear);
                    wreqr.vent.on('search:start', this.onQueryClear);
                    wreqr.vent.on('search:start', this.setupProgress);
                    wreqr.vent.on('search:start', this.showEmptyResults);
                    wreqr.vent.on('search:results', this.showResults);
                    wreqr.vent.on('metacard:selected', this.showMetacardDetail);
                    this.searchRegion.show(undefined, dir.none);
                } else {
                    wreqr.vent.off('search:show', this.showQuery);
                    wreqr.vent.off('search:clear', this.onQueryClear);
                    wreqr.vent.off('search:start', this.onQueryClear);
                    wreqr.vent.off('search:start', this.setupProgress);
                    wreqr.vent.off('search:start', this.showEmptyResults);
                    wreqr.vent.off('search:results', this.showResults);
                    wreqr.vent.off('metacard:selected', this.showMetacardDetail);
                    this.searchRegion.close();
                }
            },

            updateMapPrimitive: function() {
                wreqr.vent.trigger('search:drawend');
                if(this.query.get('north') && this.query.get('south') && this.query.get('east') &&
                    this.query.get('west')) {
                    wreqr.vent.trigger('search:bboxdisplay', this.query);
                    this.query.trigger('EndExtent');
                } else if(this.query.get('lat') && this.query.get('lon') && this.query.get('radius')) {
                    wreqr.vent.trigger('search:circledisplay', this.query);
                    this.query.trigger('EndExtent');
                }
            },

            onRender : function(){
                this.searchRegion.show(new QueryView.QueryView({ model: this.query }), dir.forward);
                this.searchControlRegion.show(new SearchControl.SearchControlView());
            },
            setupProgress: function (queryModel, progressObj) {
                if (queryModel.get('src') && queryModel.get('src').split(',').length > 1) {
                    this.progressRegion.show(new Progress.ProgressView({ resultList: queryModel.get('result'), queryModel: queryModel, model: progressObj}));
                }
            },
            onQueryClear: function () {
                if (this.progressRegion.currentView) {
                    this.progressRegion.close();
                }
                delete this.result;
            },
            showQuery: function (direction, query) {
                if(query) {
                    this.query = query;
                }
                this.searchRegion.show(new QueryView.QueryView({ sources : this.sources, model: this.query }), direction);
            },
            showEmptyResults: function() {
                this.showResults(dir.forward);
            },
            showResults: function (direction, result) {
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
            showMetacardDetail: function (direction, metacard) {
                this.searchRegion.show(new MetacardDetail.MetacardDetailView({model: metacard}), direction);
            }
        });

        return Search;
    });
