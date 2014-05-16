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

define(function (require) {
    "use strict";
    var $ = require('jquery'),
        _ = require('underscore'),
        Marionette = require('marionette'),
        SlidingRegion = require('js/view/sliding.region'),
        QueryFormView = require('js/view/Query.view').QueryView,
        ProgressView = require('js/view/Progress.view').ProgressView,
        MetacardList = require('js/view/MetacardList.view'),
        MetacardDetail = require('js/view/MetacardDetail.view'),
        MetacardModel = require('js/model/Metacard.js'),
        Backbone = require('backbone'),
        dir = require('direction'),
        ich = require('icanhaz'),
        wreqr = require('wreqr'),
        SearchControl = {};

    require('perfectscrollbar');

    ich.addTemplate('searchPanel', require('text!templates/search.panel.html'));

        SearchControl.SearchControlModel = Backbone.Model.extend({
            currentState: "search",
            initialize: function() {
                this.set({"title": "Search"});
            },
            setResultListState: function(resultList) {
                this.currentState = "results";
                this.set({"back": "Search"});
                if(resultList) {
                    if(this.resultList) {
                        this.stopListening(this.resultList, "change", this.setResults);
                    }
                    this.resultList = resultList;
                    this.listenTo(this.resultList, "change", this.setResults);
                }
                this.set({ "title": this.getResultText()});
                this.set({ "forward": ""});
                if(this.metacardDetail) {
                    this.set({ "forward": "Record"});
                }
            },
            setSearchFormState: function() {
                this.currentState = "search";
                this.set({ "title": "Search" });
                this.set({ "forward": this.getResultText()});
                this.set({"back": ""});
            },
            setRecordViewState: function(metacardDetail) {
                if(metacardDetail) {
                    this.metacardDetail = metacardDetail;
                }
                this.currentState = "record";
                this.set({ "title": "Record"});
                this.set({"back": this.getResultText()});
                this.set({ "forward": ""});
            },
            setResults: function() {
                if(this.currentState === "search") {
                    this.set({ "forward": this.getResultText()});
                } else if(this.currentState === "results") {
                    this.set({ "title": this.getResultText()});
                } else if(this.currentState === "record") {
                    this.set({"back": this.getResultText()});
                }
            },
            getResultText: function() {
                return "Results";
            }
        });

        SearchControl.SearchControlLayout = Marionette.Layout.extend({
            template : 'searchPanel',
            regions : {
                progressRegion: "#progressRegion",
                leftRegion: {
                    selector: "#searchPages",
                    regionType:  SlidingRegion
                }
            },

            events: {
                'click .back': 'back',
                'click .forward': 'forward'
            },
            
            initialize: function (options) {

                this.queryForm = new QueryFormView({
                    sources : options.sources
                });

                wreqr.vent.on('query:update', _.bind(this.updateScrollbar, this));
                wreqr.vent.on('search:clear', _.bind(this.onQueryClear, this));
                wreqr.vent.on('search:start', _.bind(this.onQueryClear, this));
                wreqr.vent.on('search:start', _.bind(this.setupProgress, this));
                wreqr.vent.on('search:start', _.bind(this.showEmptyResults, this));
                wreqr.vent.on('search:results', _.bind(this.showResults, this));
                wreqr.vent.on('metacard:selected', _.bind(this.showMetacardDetail, this));

                this.modelBinder = new Backbone.ModelBinder();
                this.controlModel = new SearchControl.SearchControlModel();
            },

            updateScrollbar: function () {
                var view = this;
                // defer seems to be necessary for this to update correctly
                _.defer(function () {
                    view.leftRegion.$el.perfectScrollbar('update');
                });
            },

            updateScrollPos: function () {
                var view = this;
                // defer seems to be necessary for this to update correctly
                _.defer(function () {
                    var selected = view.leftRegion.$el.find('.selected');
                    var container = $('#searchPages');
                    if(selected.length !== 0)
                    {
                        container.scrollTop(selected.offset().top - container.offset().top + container.scrollTop());
                    }
                });
            },

            onRender : function(){
                this.leftRegion.show(this.queryForm, dir.forward);

                var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
                this.modelBinder.bind(this.controlModel, this.$el, bindings);

                return this;
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
                $(".forward").hide();
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
                $(".back").hide();
                if(this.resultList.model.get('results').length){
                    $(".forward").show();
                }
                else {
                    $(".forward").hide();
                }
                this.controlModel.setSearchFormState();
                this.leftRegion.show(this.queryForm, direction);
            },
            showEmptyResults: function() {
                this.showResults(null, dir.forward);
            },
            showResults: function (result, direction) {
                var previousState = this.controlModel.currentState;
                if (result) {
                    if(this.resultList){
                        this.stopListening(this.resultList, 'content-update', this.updateScrollbar);
                        this.stopListening(this.resultList, 'render', this.updateScrollPos);
                    }

                    this.resultList = new MetacardList.MetacardListView({result: result});
                    this.listenTo(this.resultList, 'content-update', this.updateScrollbar);
                    this.listenTo(this.resultList, 'render', this.updateScrollPos);
                }
                if(!this.resultList){
                    this.resultList = new MetacardList.MetacardListView({result: new MetacardModel.SearchResult()});
                }
                if (previousState !== 'results' && this.leftRegion.currentView === this.queryForm && (direction !== dir.forward && direction !== dir.backward)){
                    $(".forward").show();
                } else {
                    $(".forward").hide();
                    $(".back").show();
                    if (this.metacardDetail) {
                        $(".forward").show();
                    }
                    this.controlModel.setResultListState(null);
                    this.leftRegion.show(this.resultList, direction);
                }
            },
            showMetacardDetail: function (metacard, direction) {
                $(".back").show();
                $(".forward").hide();
                this.controlModel.setRecordViewState(metacard);

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

    return SearchControl;
});
