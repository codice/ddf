
//the form should probably be a Backbone.Form but in the name of urgency I am leaving it
//as a jquery form and just wrapping it with this view
var QueryFormView = Backbone.View.extend({
    events: {
        'click #searchButton': 'search',
        'click #resetButton': 'reset',
        'click button[name=noTemporalButton]': 'noTemporalEvent',
        'click button[name=relativeTimeButton]': 'relativeTimeEvent',
        'click button[name=absoluteTimeButton]': 'absoluteTimeEvent',
        'click button[name=noLocationButton]': 'noLocationEvent',
        'click button[name=pointRadiusButton]': 'pointRadiusEvent',
        'click button[name=bboxButton]': 'bboxEvent',
        'click button[name=noTypeButton]': 'noTypeEvent',
        'click button[name=typeButton]': 'typeEvent',
        'click button[name=noFederationButton]': 'noFederationEvent',
        'click button[name=selectedFederationButton]': 'selectedFederationEvent',
        'click button[name=enterpriseFederationButton]': 'enterpriseFederationEvent'
    },
    initialize: function(options) {
        _.bindAll(this, "render", "search", "reset", "noTemporalEvent", "noTypeEvent",
            "typeEvent","relativeTimeEvent", "absoluteTimeEvent", "noLocationEvent",
            "pointRadiusEvent", "bboxEvent", "noFederationEvent", "selectedFederationEvent",
            "enterpriseFederationEvent", "updateType", "updateFederation", "updateFederationWarning",
            "updateAbsoluteTime", "updateOffset", "validatePositiveInteger", "getPositiveIntValue",
            "validateNumberInRange", "clearAbsoluteTime", "clearOffset", "clearBoundingBox",
            "clearPointRadius", "clearType", "getItemsPerPage", "getPageStartIndex", "updatePointRadius");
    },
    render: function() {
        this.$el.html(ich.searchFormTemplate());

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

        $.ajax({
            url: "/services/catalog/sources",
            dataType: "jsonp"
        }).done(function (data) {
                var sources, types, type, to, i, j, id, o;
                sources = data;
                types = {};

                for (i = 0; i < sources.length; i++) {
                    id = sources[i].id;
                    o = new Option(id, id);
                    $(o).html(id);
                    if (!sources[i].available) {
                        $(o).attr("disabled", "disabled");
                        $(o).attr("class", "disabled_option");
                    }
                    $("#federationSources").append(o);

                    for (j = 0; j < sources[i].contentTypes.length; j++) {
                        types[sources[i].contentTypes[j].name] = true;
                    }
                }

                for (type in types) {
                    to = new Option(type, type);
                    $(to).html(type);
                    $("#typeList").append(to);
                }
            });
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
        jQuery(':hidden').val('');

        $('input[name=q]').val("");
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
    noTemporalEvent: function() {
        this.clearOffset();
        this.clearAbsoluteTime();
    },
    relativeTimeEvent: function() {
        this.updateOffset();
        this.clearAbsoluteTime();
    },
    absoluteTimeEvent: function() {
        this.updateAbsoluteTime();
        this.clearOffset();
    },
    noLocationEvent: function() {
        this.clearPointRadius();
        this.clearBoundingBox();
    },
    pointRadiusEvent: function() {
        this.updatePointRadius();
        this.clearBoundingBox();
    },
    bboxEvent: function() {
        this.clearPointRadius();
        this.updateBoundingBox();
    },
    noTypeEvent: function() {
        this.clearType();
    },
    typeEvent: function() {
        this.updateType();
    },
    noFederationEvent: function() {
        $('input[name=src]').val("local");
    },
    selectedFederationEvent: function() {
        this.updateFederation();
    },
    enterpriseFederationEvent: function() {
        $('input[name=src]').val("");
    },
    updateType: function () {
        $('input[name=type]').val($('select[name=typeList]').val());
    },
    updateFederation: function () {
        var src = $("select[id=federationSources] :selected").map(function () {
            return this.value;
        }).get().join(",");
        $('input[name=src]').val(src);
        this.updateFederationWarning(src);
    },
    updateFederationWarning: function (src) {
        if (src) {
            $('#federationListWarning').hide();
        } else {
            $('#federationListWarning').show();
        }
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
    updateOffset: function() {
        var relOffsetEntry = this.validatePositiveInteger($('input[name=offsetTime]'), 1);
        if (relOffsetEntry) {
            $('input[name=dtoffset]').val(
                this.getTimeInMillis(relOffsetEntry, $(
                    'select[name=offsetTimeUnits]').val()));
            $('#timeRelativeWarning').hide();
        } else {
            this.clearOffset();
            $('#timeRelativeWarning').show();
        }
    },
    updatePointRadius: function () {
        var pointRadiusLatitude, pointRadiusLongitude, radiusValue;
        pointRadiusLatitude = this.validateNumber($('input[name=latitude]'),
            0);
        pointRadiusLongitude = this.validateNumber($('input[name=longitude]'),
            0);
        radiusValue = this.validatePositiveInteger($('input[name=radiusValue]'),
            1);

        if (pointRadiusLatitude && pointRadiusLongitude && radiusValue) {
            $('input[name=lat]').val(pointRadiusLatitude);
            $('input[name=lon]').val(pointRadiusLongitude);
            $('input[name=radius]').val(
                this.getDistanceInMeters(radiusValue, $('select[name=radiusUnits]').val()));
            $('#pointRadiusWarning').hide();
        } else {
            this.clearPointRadius();
            $('#pointRadiusWarning').show();
        }
    },
    getDistanceInMeters: function (distance, units) {

        switch (units) {
            case "meters":
                return distance;
            case "kilometers":
                return distance * 1000;
            case "feet":
                return Math.ceil(distance * 0.3048);
            case "yards":
                return Math.ceil(distance * 0.9144);
            case "miles":
                return Math.ceil(distance * 1609.34);
            default:
                return distance;
        }
    },
    validatePositiveInteger: function (posIntElement, revertValue) {
        var val = this.validateNumberInRange(0, Number.MAX_VALUE, $.trim(posIntElement.val()), revertValue, true);
        if (Number(val) === 0) {
            val = "";
        }
        val = this.getPositiveIntValue(val);
        posIntElement.val(val);
        return val;
    },
    getPositiveIntValue: function (offset) {
        var offsetValue, offsetIntValue;
        offsetValue = Number(offset);
        offsetIntValue = Math.floor(offsetValue);

        if (offsetIntValue > 0) {
            return offsetIntValue;
        } else {
            return "";
        }
    },
    validateNumberInRange: function (min, max, value, revertValue, revertIfOutOfRange) {
        var newValue = value;
        if (!value) {
            newValue = "";
        } else if (isNaN(value)) {
            newValue = revertValue;
        } else if (!isNaN(min) && Number(min) > Number(value)) {
            if (revertIfOutOfRange) {
                newValue = revertValue;
            } else {
                newValue = Number(min);
            }
        } else if (!isNaN(max) && Number(max) < Number(value)) {
            if (revertIfOutOfRange) {
                newValue = revertValue;
            } else {
                newValue = Number(max);
            }
        }

        return newValue;
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
