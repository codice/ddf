
//the form should probably be a Backbone.Form but in the name of urgency I am leaving it
//as a jquery form and just wrapping it with this view
var QueryFormView = Backbone.View.extend({
    initialize: function() {
        _.bindAll(this, "render");
    },
    render: function() {
        this.$el.html(ich.searchFormTemplate());
        return this;
    }
});

var SearchControlView = Backbone.View.extend({
    el: $('#searchControls'),
    events: {
        'click .back': 'back',
        'click .forward': 'forward',
        'click #searchButton': 'search',
        'click #resetButton': 'reset'
    },
    views: {
        'queryForm': new QueryFormView(),
        'resultList': new MetacardListView(),
        'metacardDetail': 'put the detail page here',
        'map': 'placeholder' //TODO this should go away when we do something else with the map
    },
    initialize: function() {
        _.bindAll(this, "render", "showQuery", "showResults", "search", "reset", "showMetacardDetail", "back", "forward");
        this.selectedView = "queryForm";
        this.views.map = mapView;
    },
    render: function() {
        this.$el.children('#searchPages').html(this.views[this.selectedView].render().el);
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
        $(".forwardNavText").text("Results ("+this.views.resultList.model.get("hits")+")");
        this.selectedView = "queryForm";
        this.render();
    },
    showResults: function(results) {
        $(".forward").hide();
        $(".back").show();
        $(".backNavText").text("Query");
        if(results) {
            this.views.map.createResultsOnMap(results);
            this.views.resultList = new MetacardListView({ results: results, mapView: this.mapView });
        }
        this.selectedView = "resultList";
        this.render();
    },
    showMetacardDetail: function(metacard) { //just guessing at what this method sig might be
        $(".back").show();
        $(".forward").hide();
        $(".backNavText").text("Results");
    },
    search: function() {
        //get results

        var view = this;

        $.ajax({
            url: $("#searchForm").attr("action"),
            data: $("#searchForm").serialize(),
            dataType: "jsonp",
            timeout: 300000
        }).done(function (results) {
            results.itemsPerPage = view.getItemsPerPage();
            results.startIndex = view.getPageStartIndex(1);
            view.showResults(results);
        }).fail(function () {
            showError("Failed to get results from server");
        });
    },
    reset: function() {
        jQuery(':hidden').val('');

        $('input[name=format]').val("geojson");
        $('select[name=count]').val("10");
        $('input[name=start]').val("1");

        $('button[name=noLocationButton]').click();
        clearPointRadius();
        clearBoundingBox();

        $('button[name=noTemporalButton]').click();
        clearOffset();
        clearAbsoluteTime();

        $('button[name=noFederationButton]').click();
        $('input[name=src]').val("local");

        $('button[name=noTypeButton]').click();
        clearType();
    },
    getItemsPerPage: function() {
        return parseInt($('select[name=count]').val(), 10);
    },
    getPageStartIndex: function (index) {
        return 1 + (this.getItemsPerPage() * Math.floor((index - 1) / this.getItemsPerPage()));
    }
});
