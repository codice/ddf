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
/*global define, setInterval, clearInterval*/

define([
        'backbone',
        'underscore',
        'properties',
        'moment',
        'js/model/Metacard',
        'backboneassociations'
    ],
    function (Backbone, _, properties, moment, Metacard) {
        "use strict";
        var Query = {};

        Query.Model = Backbone.AssociatedModel.extend({
            relations: [
                {
                    type: Backbone.One,
                    key: 'result',
                    relatedModel: Metacard.SearchResult
                }
            ],
            //in the search we are checking for whether or not the model
            //only contains 5 items to know if we can search or not
            //as soon as the model contains more than 5 items, we assume
            //that we have enough values to search
            defaults: {
                federation: 'enterprise',
                offsetTimeUnits: 'hours',
                scheduleUnits: 'minutes',
                timeType: 'modified',
                radiusUnits: 'meters',
                radius: 0,
                radiusValue: 0,
                count: properties.resultCount,
                startIndex: 1,
                format: "geojson"
            },

            initialize: function () {
                _.bindAll(this);
                this.listenTo(this, 'change:north change:south change:east change:west',this.setBBox);
                this.listenTo(this, 'change:scheduled change:scheduleValue change:scheduleUnits', this.startScheduledSearch);

                if(this.get('scheduled')) {
                    this.startSearch();
                }

                this.startScheduledSearch();
            },

            startScheduledSearch: function() {
                var model = this;
                if(this.get('scheduled')) {
                    var scheduleDelay = this.getScheduleDelay();
                    this.stopScheduledSearch();
                    this.timeoutId = setInterval(function () {
                        model.startSearch();
                    }, scheduleDelay);
                } else {
                    this.stopScheduledSearch();
                }
            },

            stopScheduledSearch: function() {
                if(this.timeoutId) {
                    clearInterval(this.timeoutId);
                }
            },

            getScheduleDelay: function() {
                var val;
                switch(this.get('scheduleUnits')) {
                    case 'minutes':
                        val = (this.get('scheduleValue') || 5) * 60 * 1000;
                        break;
                    case 'hours':
                        val = (this.get('scheduleValue') || 1) * 60 * 60 * 1000;
                        break;
                }
                return val;
            },

            clearSearch: function() {
                if(this.get('result')) {
                    this.get('result').cleanup();
                }
                this.set({result: undefined});
            },

            startSearch:function(progressFunction) {
                var result;
                if(this.get('result')) {
                    result = this.get('result');
                } else {
                    result = new Metacard.SearchResult();
                    this.set({result: result});
                }
                
                result.set('initiated', moment().format('lll'));

                var progress = progressFunction || function() {
                    var localResult = result;
                    localResult.get('results').each(function(searchResult) {
                        searchResult.cleanup();
                    });
                    localResult.mergeLatest();
                    localResult = null;
                };

                return result.fetch({
                    progress: progress,
                    data: this.getQueryParams(),
                    dataType: "json",
                    timeout: 300000,
                    error : function(){
                        if (typeof console !== 'undefined') {
                            console.error(arguments);
                        }
                    }
                });
            },

            setSources: function(sources) {
                var sourceArr = [];
                sources.each(function (src) {
                    if (src.get('available') === true) {
                        sourceArr.push(src.get('id'));
                    }
                });
                if (sourceArr.length > 0) {
                    this.set('src', sourceArr.join(','));
                }
            },

            getQueryParams: function () {
                var queryParams = this.toJSON();
                queryParams.count = this.get("count");
                queryParams.start = this.get("startIndex");
                queryParams.format = this.get("format");
                return queryParams;
            },

            setDefaults : function() {
                var model = this;
                _.each(_.keys(model.defaults), function(key) {
                    model.set(key, model.defaults[key]);
                });
            },

            setBBox : function() {
                var north = this.get('north'),
                    south = this.get('south'),
                    west = this.get('west'),
                    east = this.get('east');
                if(north && south && east && west){
                    this.set('bbox', [west,south,east,north].join(','));
                }
            },

            swapDatesIfNeeded : function() {
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
        return Query;

    });