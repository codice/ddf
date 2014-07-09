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
/*global define*/

define([
        'backbone',
        'underscore',
        'properties',
        'js/model/Metacard',
        'backbonerelational'
    ],
    function (Backbone, _, properties, Metacard) {
        "use strict";
        var Query = {};

        Query.Model = Backbone.RelationalModel.extend({
            relations: [
                {
                    type: Backbone.HasOne,
                    key: 'result',
                    relatedModel: Metacard.SearchResult,
                    includeInJSON: true
                }
            ],
            //in the search we are checking for whether or not the model
            //only contains 5 items to know if we can search or not
            //as soon as the model contains more than 5 items, we assume
            //that we have enough values to search
            defaults: {
                federation: 'enterprise',
                offsetTimeUnits: 'hours',
                timeType: 'modified',
                radiusUnits: 'meters',
                radius: 0,
                radiusValue: 0,
                count: properties.resultCount,
                startIndex: 1,
                format: "geojson"
            },

            initialize: function () {
                this.on('change:north change:south change:east change:west',this.setBBox);
                _.bindAll(this);
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