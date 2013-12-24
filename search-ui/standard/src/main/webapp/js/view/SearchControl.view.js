/*global define*/

define(function (require) {
    "use strict";
    var $ = require('jquery'),
        _ = require('underscore'),
        Marionette = require('marionette'),
        SlidingRegion = require('js/view/sliding.region'),
        QueryFormView = require('js/view/Query.view').QueryView,
        CesiumMetacard = require('js/view/cesium.metacard'),
        MetacardList = require('js/view/MetacardList.view'),
        Metacard = require('js/view/MetacardDetail.view'),
        Backbone = require('backbone'),
        ddf = require('ddf'),
        dir = require('direction'),
        ich = require('icanhaz'),
        SearchControl = {};

    require('perfectscrollbar');

    ich.addTemplate('searchPanel', require('text!templates/search.panel.html'));

        SearchControl.SearchControlModel = Backbone.Model.extend({

        });

        SearchControl.SearchControlLayout = Marionette.Layout.extend({
            template : 'searchPanel',
            regions : {
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
                this.listenTo(this.queryForm, 'content-update', this.updateScrollbar);

                this.listenTo(this.queryForm, 'clear', this.onQueryClear);
                this.listenTo(this.queryForm, 'searchComplete', this.showResults);
                this.listenTo(this.queryForm, 'searchComplete', this.changeDefaultMapLocation);
                this.listenTo(ddf.app, 'model:context', this.showMetacardDetail);
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

                return this;
            },
            onQueryClear: function () {
                $(".forward").hide();
                if (this.mapViews) {
                    this.mapViews.close();
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
                    this.showResults(null, dir.forward);
                }
                else if (this.leftRegion.currentView === this.resultList) {
                    this.showMetacardDetail(null, dir.forward);
                }
            },
            changeDefaultMapLocation: function (result, shouldFlyToExtent) {
                console.log("changing "+result);
                if(shouldFlyToExtent) {
                    var extent = result.getResultCenterPoint();
                    if(extent) {
                        ddf.app.controllers.geoController.flyToExtent(extent);
                    }
                }
            },
            showQuery: function (direction) {
                $(".back").hide();
                $(".forward").show();
                this.leftRegion.show(this.queryForm, direction);
                if (this.resultList) {
                    $(".forwardNavText").text("Results (" + this.resultList.model.get("hits") + ")");
                }
                $(".nav-title").text("Query");
            },
            showResults: function (result, direction) {
                $(".forward").hide();
                $(".back").show();
                $(".backNavText").text("Query");
                if (this.metacardDetail) {
                    $(".forwardNavText").text("Record");
                    $(".forward").show();
                }
                if (result) {
                    // TODO replace with trigger
                    if (this.mapViews) {
                        this.mapViews.close();
                    }
                    if (ddf.app.controllers.geoController.enabled) {
                        this.mapViews = new CesiumMetacard.ResultsView({
                            collection: result.get('results'),
                            geoController: ddf.app.controllers.geoController
                        }).render();
                    }
                    if(this.resultList){
                        this.stopListening(this.resultList, 'content-update', this.updateScrollbar);
                        this.stopListening(this.resultList, 'render', this.updateScrollPos);
                    }

                    this.resultList = new MetacardList.MetacardListView({ result: result, searchControlView: this });
                    this.listenTo(this.resultList, 'content-update', this.updateScrollbar);
                    this.listenTo(this.resultList, 'render', this.updateScrollPos);
                }
                this.leftRegion.show(this.resultList, direction);
                $(".nav-title").text("Results (" + this.resultList.model.get("hits") + ")");
            },
            showMetacardDetail: function (metacard, direction) {
                $(".back").show();
                $(".forward").hide();
                $(".backNavText").text("Results (" + this.resultList.model.get("hits") + ")");
                $(".nav-title").text("Record");

                if (!metacard && this.metacardDetail) {
                    this.metacardDetail.model.set('direction', dir.forward);
                    metacard = this.metacardDetail.model;
                }

                if (metacard) {
                    if(this.metacardDetail){
                        this.stopListening(this.metacardDetail, 'content-update', this.updateScrollbar);
                    }
                    this.metacardDetail = new Metacard.MetacardDetailView({metacard: metacard});
                    this.listenTo(this.metacardDetail, 'content-update', this.updateScrollbar);
                    direction = _.isUndefined(metacard.get('direction')) ? direction : metacard.get('direction');
                }

                this.leftRegion.show(this.metacardDetail, direction);
            }
        });

    return SearchControl;
});
