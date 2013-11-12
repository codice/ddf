
var SearchControlView = Backbone.View.extend({
    el: $('#searchControls'),
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
        this.views.map = mapView;
    },
    render: function() {
        this.$el.children("#searchPages").append(this.views[this.selectedView].render().el);
        return this;
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
//        else if(this.selectedView === "resultList")
//        {
//            //no forward here
//        }
//        else if(this.selectedView === "metacardDetail")
//        {
//            //no forward here
//        }
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
        if(result) {
            this.views.map.createResultsOnMap(result);
            if(this.views.resultList) {
                this.views.resultList.close();
            }
            this.views.resultList = new MetacardListView({ result: result, mapView: this.mapView, searchControlView: this });
        }
        this.views.resultList.$el.show();
        $(".centerNavText").text("Results ("+this.views.resultList.model.get("hits")+")");
        this.selectedView = "resultList";
        this.render();
    },
    showMetacardDetail: function(metacard) { //just guessing at what this method sig might be
        $(".back").show();
        $(".forward").hide();
        $(".backNavText").text("Results");
        $(".centerNavText").text("Metacard");
        this.selectedView = "metacardDetail";
    }
});
