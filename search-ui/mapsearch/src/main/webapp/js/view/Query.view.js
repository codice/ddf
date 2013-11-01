
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
        'click #backButton' : 'back',
        'click #forwardButton' : 'forward',
        'click #searchButton': 'search',
        'click #resetButton': 'reset'
    },
    views: [
        new QueryFormView(),
        new MetacardListView()
    ],
    mapView: new MapView(),
    initialize: function() {
        _.bindAll(this, "render", "back", "forward", "search", "reset");
        this.selectedView = this.views[0];
    },
    render: function() {
        this.$el.html(this.selectedView.render().el);
        return this;
    },
    back: function() {
        this.selectedView = this.views[0];
        this.render();
    },
    forward: function(results) {
        if(results) {
            if(this.views[1])
            {
                this.views[1].destroy();
            }
            this.views[1] = new MetacardListView(results);
            if(this.mapView)
            {
                this.mapView.destroy();
            }
            this.mapView = new MapView(results);
        }
        this.selectedView = this.views[1];
        this.render();
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
            view.forward(results);
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
        return 1 + (getItemsPerPage() * Math.floor((index - 1) / getItemsPerPage()));
    }
});
