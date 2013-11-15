/*global define*/

define(function (require) {
    "use strict";
    var $ = require('jquery'),
        Backbone = require('backbone'),
        _ = require('underscore'),
        QueryFormView = require('js/view/Query.view'),
        CesiumMetacard = require('js/view/cesium.metacard'),
        MetacardList = require('js/view/MetacardList.view'),
        Metacard = require('js/view/MetacardDetail.view'),
        ddf = require('ddf'),


    SearchControlView = Backbone.View.extend({

    events: {
        'click .back': 'back',
        'click .forward': 'forward'
    },
    views: {
    },
    initialize: function() {
        _.bindAll(this, "render", "showQuery", "showResults", "showMetacardDetail", "back", "forward");
        this.selectedView = "queryForm";
        this.views.queryForm = new QueryFormView({searchControlView: this});
        this.listenTo(this.views.queryForm, 'clear', this.onQueryClear);
    },
    render: function() {
        this.$el.children("#searchPages").append(this.views[this.selectedView].render().el);
        return this;
    },
    onQueryClear: function () {
        $(".forward").hide();
        if(this.views.mapViews){
            this.views.mapViews.close();
        }
    },
    back: function() {
//        if(this.selectedView === "queryForm")
//        {
//            //we don't have anything behind this page yet
//        }
        if(this.selectedView === "resultList")
        {
            //go back to query
            this.showQuery();
        }
        else if(this.selectedView === "metacardDetail")
        {
            this.showResults();
        }
    },
    forward: function() {
        if(this.selectedView === "queryForm")
        {
            this.showResults();
        }
        else if(this.selectedView === "resultList")
        {
            this.showMetacardDetail();
        }
    },
    showQuery: function() {
        $(".back").hide();
        $(".forward").show();
        this.views.queryForm.$el.show();
        if(this.views.resultList)
        {
            this.views.resultList.$el.hide();
            $(".forwardNavText").text("Results ("+this.views.resultList.model.get("hits")+")");
        }
        $(".centerNavText").text("Query");
        this.selectedView = "queryForm";
        this.render();
    },
    showResults: function(result) {
        $(".forward").hide();
        $(".back").show();
        $(".backNavText").text("Query");
        this.views.queryForm.$el.hide();
        if(this.views.metacardDetail) {
            this.views.metacardDetail.$el.hide();
            $(".forwardNavText").text("Metacard");
            $(".forward").show();
        }
        if(result) {
            if(this.views.mapViews){
                this.views.mapViews.close();
            }
            this.views.mapViews = new CesiumMetacard.ResultsView({
                collection : result.get('results'),
                geoController : ddf.app.controllers.geoController
            }).render();

//            this.views.map.createResultsOnMap();
            if(this.views.resultList) {
                this.views.resultList.close();
            }
            this.views.resultList = new MetacardList.MetacardListView({ result: result,searchControlView: this });
        }
        this.views.resultList.$el.show();
        $(".centerNavText").text("Results ("+this.views.resultList.model.get("hits")+")");
        this.selectedView = "resultList";
        this.render();
    },
    showMetacardDetail: function(metacard) {
        $(".back").show();
        $(".forward").hide();
        $(".backNavText").text("Results ("+this.views.resultList.model.get("hits")+")");
        $(".centerNavText").text("Metacard");
        this.views.resultList.$el.hide();
        if (metacard) {
            if(this.views.metacardDetail) {
                this.views.metacardDetail.close();
            }
            this.views.metacardDetail = new Metacard.MetacardDetailView({metacard: metacard});
        }
        this.views.metacardDetail.$el.show();
        this.selectedView = "metacardDetail";
        this.render();
    }
});

    return SearchControlView;
});
