/*global define*/

define(function (require) {
    "use strict";
    var $ = require('jquery'),
        Backbone = require('backbone'),
        Marionette = require('marionette'),
        _ = require('underscore'),
        ich = require('icanhaz'),
        ddf = require('ddf'),
        MetaCard = require('js/model/Metacard'),
        Spinner = require('spin'),
        Query = {};

    ich.addTemplate('searchFormTemplate', require('text!templates/searchForm.handlebars'));
    require('datepickerOverride');
    require('datepickerAddon');
    require('modelbinder');


    Query.Model = Backbone.Model.extend({
        initialize: function () {
            this.on('change', this.log);
            this.on('change:north change:south change:east change:west',this.setBBox);
            _.bindAll(this,'swapDatesIfNeeded');
        },
        log: function () {
            console.log(this.toJSON());
        },

        setBBox : function(){
            var north = this.get('north'),
                south = this.get('south'),
                west = this.get('west'),
                east = this.get('east');
            if(north && south && east && west){
                this.set('bbox', [west,south,east,north].join(','));
            }

        },

        swapDatesIfNeeded : function(){
            var model = this;
            if(model.get('dtstart') && model.get('dtend')){
                var start = new Date(model.get('dtstart'));
                var end = new Date(model.get('dtend'));
                if(start > end){
                    this.set({
                        dtstart : end.toISOString(),
                        dtend : start.toISOString()
                    });
                }
            }
        }



    });


    Query.QueryView = Marionette.ItemView.extend({
        template: 'searchFormTemplate',
        className : 'slide-animate',

        events: {
            'click .searchButton': 'search',
            'click .resetButton': 'reset',
            'click .time': 'clearTime',
            'click .location': 'clearLocation',
            'click .type': 'clearType',
            'click .federation': 'clearFederation',
            'click button[name=pointRadiusButton]' : 'drawCircle',
            'click button[name=bboxButton]' : 'drawExtent',
            'click button[name=noFederationButton]' : 'noFederationEvent',
            'click button[name=selectedFederationButton]': 'selectedFederationEvent',
            'click button[name=enterpriseFederationButton]': 'enterpriseFederationEvent',
            'keypress input[name=q]': 'filterOnEnter',
            'change #radiusUnits': 'onRadiusUnitsChanged',
            'change #offsetTimeUnits': 'onTimeUnitsChanged'


        },


        initialize: function (options) {
            _.bindAll(this);
            this.model = new Query.Model();
            this.modelBinder = new Backbone.ModelBinder();
            this.sources = options.sources;
        },

        noFederationEvent : function(){
            this.model.set('src','local');
            this.updateScrollbar();
        },

        enterpriseFederationEvent : function(){
            this.model.unset('src');
            this.updateScrollbar();
        },

        selectedFederationEvent : function(){
            this.model.unset('src');
            this.updateScrollbar();
        },

        clearTime: function () {
            this.model.set({
                dtstart: undefined,
                dtend: undefined,
                dtoffset: undefined
            }, {unset: true});
            this.updateScrollbar();
        },

        clearLocation: function () {
            this.model.set({
                north: undefined,
                east: undefined,
                west: undefined,
                south: undefined,
                lat: undefined,
                lon: undefined,
                radius: undefined,
                bbox : undefined
            }, {unset: true});
            ddf.app.controllers.drawCircleController.stop();
            ddf.app.controllers.drawExentController.stop();
            this.updateScrollbar();
        },

        clearType: function () {
            this.model.set({
                type: undefined
            }, {unset: true});
            this.updateScrollbar();
        },

        updateScrollbar: function () {
            this.trigger('content-update');
        },

        serializeData: function () {
            var allTypes = _.chain(this.sources.map(function (source) {
                return source.get('contentTypes');
            })).flatten().unique().value();
            var allSources = this.sources.toJSON();
            return _.extend(this.model.toJSON(), {types: allTypes, sources: allSources});
        },

        onRender: function () {
            var units = this.$('#radiusUnits'),
                view = this;

            var radiusConverter = function (direction, value) {
                    var unitVal = units.val();
                    if (direction === 'ModelToView') {
                        return view.getDistanceFromMeters(value, unitVal);
                    }
                    else {
                        return view.getDistanceInMeters(value, unitVal);
                    }
                },
                offsetConverter = function (direction, value) {
                    if (direction !== 'ModelToView') {
                        //all we really need to do is just calculate the dtoffset on the model based on these two values
                        view.model.set("dtoffset", view.getTimeInMillis(view.$("input[name=offsetTime]").val(), view.$("select[name=offsetTimeUnits]").val()));
                    }
                    return value;
                },
                federationConverter = function(direction,value){
                    if(value && _.isArray(value)){
                        return value.join(',');
                    }
                    return value;
                };

            var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
            bindings.radius.selector = 'input[name=radiusValue]';
            bindings.radius.converter = radiusConverter;
            bindings.offsetTime.converter = offsetConverter;
            bindings.offsetTimeUnits.converter = offsetConverter;
            bindings.src = {};
            bindings.src.selector = '#federationSources';
            bindings.src.converter =  federationConverter;


            this.modelBinder.bind(this.model, this.$el, bindings);

            this.$('#absoluteStartTime').datetimepicker({
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
                onClose: this.model.swapDatesIfNeeded
            });

            this.$('#absoluteEndTime').datetimepicker({
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
                onClose: this.model.swapDatesIfNeeded
            });
            this.delegateEvents();
        },


        drawCircle: function(){
            ddf.app.controllers.drawCircleController.draw(this.model);
        },

        drawExtent : function(){
            ddf.app.controllers.drawExentController.drawExtent(this.model);
        },

        onClose: function () {
            this.modelBinder.unbind();
        },

        filterOnEnter: function (e) {
            var view = this;
            if (e.keyCode === 13) {
                // defer it to make sure the event to set the query parameter is run
                _.defer(function () {
                    view.search();
                });
            }

        },

        search: function () {
            //get results
            var queryParams, view = this, result, options;
            queryParams = this.model.toJSON();
            options = {
                'queryParams': $.param(queryParams)
            };

            result = new MetaCard.SearchResult(options);

            var spinner = new Spinner(spinnerOpts).spin(this.el);
            // disable the whole form
            this.$('button').addClass('disabled');
            this.$('input').prop('disabled',true);
            result.fetch({

                data: result.getQueryParams(),
                dataType: "jsonp",
                timeout: 300000,
                error : function(){
                    spinner.stop();
                    console.error(arguments);
                }
            }).complete(function () {
                    spinner.stop();
                    //re-enable the whole form
                    view.$('button').removeClass('disabled');
                    view.$('input').prop('disabled',false);
                    view.trigger('searchComplete', result);
                });


        },
        reset: function () {
            this.model.clear();
            this.trigger('clear');
        },

        onRadiusUnitsChanged: function () {
            this.$('input[name=radiusValue]').val(
                this.getDistanceFromMeters(this.model.get('radius'), this.$('#radiusUnits').val()));
        },

        onTimeUnitsChanged : function(){
            var timeInMillis = this.getTimeInMillis(this.model.get('dtoffset'), this.$('#offsetTimeUnits').val());
            // silently set it so as not to trigger a modelbinder update
            this.model.set('dtoffset', timeInMillis, {silent:true});
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
        getDistanceFromMeters: function (distance, units) {
            switch (units) {
                case "meters":
                    return distance;
                case "kilometers":
                    return distance / 1000;
                case "feet":
                    return Math.ceil(distance / 0.3048);
                case "yards":
                    return Math.ceil(distance / 0.9144);
                case "miles":
                    return Math.ceil(distance / 1609.34);
                default:
                    return distance;
            }
        },

        getTimeInMillis: function (val, units) {
            switch (units) {
                case "seconds":
                    return val * 1000;
                case "minutes":
                    return this.getTimeInMillis(val, 'seconds') * 60;
                case "hours":
                    return this.getTimeInMillis(val, 'minutes') * 60;
                case "days":
                    return this.getTimeInMillis(val, 'hours') * 24;
                case "weeks":
                    return this.getTimeInMillis(val, 'days') * 7;
                case "months":
                    return this.getTimeInMillis(val, 'weeks') * 4;
                case "years" :
                    return this.getTimeInMillis(val, 'days') * 365;
                default:
                    return val;
            }
        },

        getTimeFromMillis: function (val, units) {
            switch (units) {
                case "seconds":
                    return val / 1000;
                case "minutes":
                    return this.getTimeFromMillis(val, 'seconds') / 60;
                case "hours":
                    return this.getTimeFromMillis(val, 'minutes') / 60;
                case "days":
                    return this.getTimeFromMillis(val, 'hours') / 24;
                case "weeks":
                    return this.getTimeFromMillis(val, 'days') / 7;
                case "months":
                    return this.getTimeFromMillis(val, 'weeks') / 4;
                case "years" :
                    return this.getTimeFromMillis(val, 'days') / 365;
                default:
                    return val;
            }
        }


    });

    var spinnerOpts = {
        lines: 13, // The number of lines to draw
        length: 12, // The length of each line
        width: 10, // The line thickness
        radius: 30, // The radius of the inner circle
        corners: 1, // Corner roundness (0..1)
        rotate: 0, // The rotation offset
        direction: 1, // 1: clockwise, -1: counterclockwise
        color: '#929292', // #rgb or #rrggbb or array of colors
        speed: 1, // Rounds per second
        trail: 60, // Afterglow percentage
        shadow: false, // Whether to render a shadow
        hwaccel: false, // Whether to use hardware acceleration
        className: 'spinner', // The CSS class to assign to the spinner
        zIndex: 2e9, // The z-index (defaults to 2000000000)
        top: 'auto', // Top position relative to parent in px
        left: 'auto' // Left position relative to parent in px
    };

    return Query;

});


