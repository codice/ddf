
//the form should probably be a Backbone.Form but in the name of urgency I am leaving it
//as a jquery form and just wrapping it with this view
var QueryFormView = Backbone.View.extend({
    events: {
        'click #searchButton': 'search',
        'click #resetButton': 'reset'
    },
    initialize: function(options) {
        _.bindAll(this, "render");
        $('#absoluteStartTime').datetimepicker({
            dateFormat: $.datepicker.ATOM,
            timeFormat: "HH:mm:ss.lz",
            separator: "T",
            timezoneIso8601: true,
            useLocalTimezone: true,
            showHour: false,
            showMinute: false,
            showSecond: false,
            showMillisec: false,
            showTimezone: false,
            minDate: new Date(100, 0, 2),
            maxDate: new Date(9999, 11, 30),
            onClose: this.updateAbsoluteTime
        });

        $('#absoluteEndTime').datetimepicker({
            dateFormat: $.datepicker.ATOM,
            timeFormat: "HH:mm:ss.lz",
            separator: "T",
            timezoneIso8601: true,
            useLocalTimezone: true,
            showHour: false,
            showMinute: false,
            showSecond: false,
            showMillisec: false,
            showTimezone: false,
            minDate: new Date(100, 0, 2),
            maxDate: new Date(9999, 11, 30),
            onClose: this.updateAbsoluteTime
        });
    },
    render: function() {
        this.$el.html(ich.searchFormTemplate());
        return this;
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
                view.options.searchControlView.showResults(results);
            }).fail(function () {
                showError("Failed to get results from server");
            });
    },
    reset: function() {
        $(':hidden').val('');

        $('input[name=format]').val("geojson");
        $('select[name=count]').val("10");
        $('input[name=start]').val("1");

        $('button[name=noLocationButton]').click();
        //point radius
        this.clearPointRadius();

        //bounding box
        this.clearBoundingBox();

        $('button[name=noTemporalButton]').click();
        //offset
        this.clearOffset();

        //absolute time
        this.clearAbsoluteTime();

        $('button[name=noFederationButton]').click();
        $('input[name=src]').val("local");

        $('button[name=noTypeButton]').click();
        this.clearType();
    },
    updateAbsoluteTime: function() {
        var start, end;

        start = $('input[name=absoluteStartTime]').datepicker("getDate");
        end = $('input[name=absoluteEndTime]').datepicker("getDate");

        if (start && end) {
            if (start > end) {
                $('input[name=absoluteStartTime]').datepicker("setDate", end);
                $('input[name=absoluteEndTime]').datepicker("setDate", start);
            }

            $('input[name=dtstart]').val($('input[name=absoluteStartTime]').val());
            $('input[name=dtend]').val($('input[name=absoluteEndTime]').val());

            $('#timeAbsoluteWarning').hide();
        } else {
            $('#timeAbsoluteWarning').show();
            this.clearAbsoluteTime();
        }
    },
    clearOffset: function() {
        $('input[name=dtoffset]').val("");
    },
    clearAbsoluteTime: function() {
        $('input[name=dtstart]').val("");
        $('input[name=dtend]').val("");
    },
    clearPointRadius: function() {
        $('input[name=lat]').val("");
        $('input[name=lon]').val("");
        $('input[name=radius]').val("");
    },
    clearBoundingBox: function() {
        $('input[name=bbox]').val("");
    },
    clearType: function() {
        $('input[name=type]').val("");
    },
    getItemsPerPage: function() {
        return parseInt($('select[name=count]').val(), 10);
    },
    getPageStartIndex: function(index) {
        return 1 + (this.getItemsPerPage() * Math.floor((index - 1) / this.getItemsPerPage()));
    }
});

var SearchControlView = Backbone.View.extend({
    el: $('#searchControls'),
    events: {
        'click .back': 'back',
        'click .forward': 'forward'
    },
    views: {
        'queryForm': 'queryForm',
        'resultList': 'resultList',
        'metacardDetail': 'metacardDetail',
        'map': 'placeholder' //TODO this should go away when we do something else with the map
    },
    initialize: function() {
        _.bindAll(this, "render", "showQuery", "showResults", "showMetacardDetail", "back", "forward");
        this.selectedView = "queryForm";
        this.views.queryForm = new QueryFormView({searchControlView: this, el: this.$el.children('#searchPages')});
        this.views.resultList = new MetacardListView({searchControlView: this, el: this.$el.children('#searchPages')});
        this.views.map = mapView;
    },
    render: function() {
        this.views[this.selectedView].render();
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
            this.views.resultList = new MetacardListView({ results: results, mapView: this.mapView, searchControlView: this, el: this.$el.children('#searchPages') });
        }
        this.selectedView = "resultList";
        this.render();
    },
    showMetacardDetail: function(metacard) { //just guessing at what this method sig might be
        $(".back").show();
        $(".forward").hide();
        $(".backNavText").text("Results");
    }
});
