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
        ddf = require('ddf'),
        ich = require('icanhaz');

    require('perfectscrollbar');

    var forward = true,
        backward = false;
    ich.addTemplate('searchPanel', require('text!templates/search.panel.html'));

        var SearchControlLayout = Marionette.Layout.extend({
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
                this.leftRegion.show(this.queryForm, forward);

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
                    this.showQuery(backward);
                }
                else if (this.leftRegion.currentView === this.metacardDetail) {
                    this.showResults(null, backward);
                }
            },
            forward: function () {
                if (this.leftRegion.currentView === this.queryForm) {
                    this.showResults(null,forward);
                }
                else if (this.leftRegion.currentView === this.resultList) {
                    this.showMetacardDetail(null, forward);
                }
            },
            showQuery: function (direction) {
                $(".back").hide();
                $(".forward").show();
                this.leftRegion.show(this.queryForm, direction);
                if (this.resultList) {
                    $(".forwardNavText").text("Results (" + this.resultList.model.get("hits") + ")");
                }
                $(".centerNavText").text("Query");
            },
            showResults: function (result, direction) {
                $(".forward").hide();
                $(".back").show();
                $(".backNavText").text("Query");
                if (this.metacardDetail) {
                    $(".forwardNavText").text("Metacard");
                    $(".forward").show();
                }
                if (result) {
                    // TODO replace with trigger
                    if (this.mapViews) {
                        this.mapViews.close();
                    }
                    this.mapViews = new CesiumMetacard.ResultsView({
                        collection: result.get('results'),
                        geoController: ddf.app.controllers.geoController
                    }).render();
                    if(this.resultList){
                        this.stopListening(this.resultList, 'content-update', this.updateScrollbar);
                        this.stopListening(this.resultList, 'render', this.updateScrollPos);
                    }

                    this.resultList = new MetacardList.MetacardListView({ result: result, searchControlView: this });
                    this.listenTo(this.resultList, 'content-update', this.updateScrollbar);
                    this.listenTo(this.resultList, 'render', this.updateScrollPos);
                }
                this.leftRegion.show(this.resultList, direction);
                $(".centerNavText").text("Results (" + this.resultList.model.get("hits") + ")");
            },
            showMetacardDetail: function (metacard, direction) {
                $(".back").show();
                $(".forward").hide();
                $(".backNavText").text("Results (" + this.resultList.model.get("hits") + ")");
                $(".centerNavText").text("Metacard");
                if (metacard) {
                    if(this.metacardDetail){
                        this.stopListening(this.metacardDetail, 'content-update', this.updateScrollbar);
                    }
                    this.metacardDetail = new Metacard.MetacardDetailView({metacard: metacard});
                    this.listenTo(this.metacardDetail, 'content-update', this.updateScrollbar);
                }
                this.leftRegion.show(this.metacardDetail, direction);
            }
        });

    return SearchControlLayout;
});
