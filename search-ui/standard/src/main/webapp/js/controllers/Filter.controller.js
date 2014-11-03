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
/*global define */
define([
    'underscore',
    'marionette',
    'wreqr',
    'moment',
    'js/model/source',
    'properties'
], function (_, Marionette, wreqr, moment, Source, Properties) {
        'use strict';
        var FilterController;

        FilterController = Marionette.Controller.extend({
            initialize: function (options) {
                _.bindAll(this);
                this.query = options.query;

                this.fields = [];
                this.facetCounts = {};
                this.showFilterFlag = false;

                wreqr.reqres.setHandler('getFields', this.getFields);
                wreqr.reqres.setHandler('getFacetCounts', this.getFacetCounts);
                wreqr.reqres.setHandler('getShowFilterFlag', this.getShowFilterFlag);
                wreqr.reqres.setHandler('getSourcePromise', this.getSourcePromise);
                this.listenTo(wreqr.vent,'processSearch', this.processSearch);
                this.listenTo(wreqr.vent,'filterFlagChanged', this.filterFlagChanged);

            },

            getShowFilterFlag: function(){
                return this.showFilterFlag;
            },

            getFacetCounts: function(){
                return this.facetCounts;
            },

            getFields: function(){
                return this.fields;
            },

            filterFlagChanged: function(isFilterShown){
                this.showFilterFlag = isFilterShown;
            },

            getSourcePromise: function(){
                var sources = new Source.Collection();
                return sources.fetchPromise();
            },

            processSearch: function(searchToProcess){
                // default all field
                var array = [
                    {name: 'anyText', type: 'string', displayName: 'Any Text'},
                    {name: 'anyGeo', type: 'anyGeo', displayName: 'Any Geo'}
                ];
                var facetCounts = {};

                // process the types returned in this query.
                if(searchToProcess.has('metacard-types')){
                    var types = searchToProcess.get('metacard-types');
                    _.each(_.keys(types), function(type){
                        var pairs = _.pairs(types[type]);
                        var currentFieldNames = _.pluck(array, 'name');
                        _.each(pairs, function(pair){
                            if(!_.contains(currentFieldNames, pair[0])){
                                // doesn't exist.  lets add.
                                var fieldObj = {
                                    name: pair[0],
                                    type: pair[1].toLowerCase()
                                };
                                array.push(fieldObj);
                            }
                        });
                    });
                }


                // this give us our facet counts.
                if(searchToProcess.get('results')){
                    searchToProcess.get('results').each(function(item){
                        var pairs = item.get('metacard').get('properties').pairs();
                        _.each(pairs, function(pair){

                            // lets add the field-facet if it does not exist.
                            if(!_.has(facetCounts, pair[0])){
                                facetCounts[pair[0]] = {};
                            }
                            // lets increment the facet value.  If none exist, create one with value of 1.
                            var curValue = pair[1];
                            if(_.has(facetCounts[pair[0]],curValue)){
                                facetCounts[pair[0]][curValue]++;
                            } else {
                                facetCounts[pair[0]][curValue] = 1;
                            }
                        });
                    });
                }
                this.registerFields(array);
                this.registerFacetCounts(facetCounts);
            },

            registerFields: function(newFields){
                var that = this;
                _.each(newFields, function(newField){
                    var currentFieldNames = _.pluck(that.fields, 'name');
                    if(!_.contains(currentFieldNames, newField.name)){
                        that.fields.push(newField);
                    }
                });
            },

            registerFacetCounts: function(facetCounts){
                var controller = this;
                var contentTypes = wreqr.reqres.request("workspace:gettypes");
                var contentTypeIds = contentTypes.pluck('name');
                controller.facetCounts = _.pick(facetCounts,[Properties.filters.METADATA_CONTENT_TYPE]);
                var defaults = {};
                defaults[Properties.filters.METADATA_CONTENT_TYPE] = {};
                controller.facetCounts = _.extend(defaults, controller.facetCounts);
                var keys = _.keys(controller.facetCounts);
                 _.each(keys, function(key){
                    if(key === Properties.filters.METADATA_CONTENT_TYPE){
                        _.each(contentTypeIds, function(contentTypeId){
                            if(!_.has(controller.facetCounts[key], contentTypeId)){
                                controller.facetCounts[key][contentTypeId] = 0;
                            }
                        });
                    }
                });
            }
        });

        return FilterController;
    }
);
