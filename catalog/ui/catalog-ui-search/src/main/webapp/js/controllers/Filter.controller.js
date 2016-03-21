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
    'backbone',
    'marionette',
    'wreqr',
    'moment',
    'js/model/source',
    'properties',
    'js/store'
], function (_, Backbone, Marionette, wreqr, moment, Source, Properties, store) {
        'use strict';
        var FilterController;

        FilterController = Marionette.Controller.extend({
            initialize: function () {
                _.bindAll(this);

                this.fields = new Backbone.Collection([],{
                    comparator: function(model){
                        var name = model.get('name');
                        if(name === 'anyText'){
                            // anyText first
                            return [1,name];
                        } else if(name === 'anyGeo'){
                            // anyGeo Second
                            return [2,name];
                        }

                        // the rest order by name.
                        return [3,name];
                    }
                });
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
                return this.fields.toJSON();
            },

            filterFlagChanged: function(isFilterShown){
                this.showFilterFlag = isFilterShown;
            },

            getSourcePromise: function(){
                var sources = new Source.Collection();
                return sources.fetchPromise();
            },

            processSearch: function(searchToProcess) {
                // send out filters off to be displayed where ever.
                wreqr.vent.trigger('mapfilter:showFilters', searchToProcess.parents[0].filters);
                // default all field
                var array = [
                    {name: 'anyText', type: 'string', displayName: 'Any Text'},
                    {name: 'anyGeo', type: 'geometry', displayName: 'Any Geo'}
                ];
                var facetCounts = {};

                // process the types returned in this query.
                if (searchToProcess.has('metacard-types')) {
                    var types = searchToProcess.get('metacard-types');
                    _.each(_.keys(types), function (type) {
                        var pairs = _.pairs(types[type]);
                        var currentFieldNames = _.pluck(array, 'name');
                        _.each(pairs, function (pair) {
                            if (!_.contains(currentFieldNames, pair[0]) && pair[1].indexed === true) {
                                // doesn't exist.  lets add.
                                var fieldType = pair[1].format.toLowerCase();
                                // lets convert to a number type.
                                if (_.contains(Properties.filters.numberTypes, fieldType)) {
                                    fieldType = 'number';
                                }

                                var fieldObj = {
                                    name: pair[0],
                                    type: fieldType
                                };
                                array.push(fieldObj);
                            }
                        });
                    });
                }

                var numberOfResults = 0;
                if (searchToProcess.get('results')) {
                    numberOfResults = searchToProcess.get('results').length;
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
                                // facet value exists already.  increment.
                                facetCounts[pair[0]][curValue]++;
                            } else {
                                if(pair[0] === Properties.filters.METADATA_CONTENT_TYPE){
                                    // new facet value for metadata-content-type.  lets find the custom field it belongs to.
                                    // note: the target facet value may not be mapped.
                                    var customFields = [];  // using array to support for a single content type to be assigned to multiple groups.
                                    _.each(_.keys(Properties.typeNameMapping), function(key){
                                        _.each(Properties.typeNameMapping[key], function(value){
                                            if(value === curValue){
                                                customFields.push(key);
                                            }
                                        });
                                    });
                                    if(!_.isEmpty(customFields)){
                                        _.each(customFields, function(customField){
                                            // its a custom field, determine if it needs to be created or incremented.
                                            if(_.has(facetCounts[pair[0]],customField)){
                                                facetCounts[pair[0]][customField]++;
                                            } else {
                                                // create custom facet value.
                                                facetCounts[pair[0]][customField] = 1;
                                            }
                                        });
                                    } else {
                                        // create non-custom facet value.
                                        facetCounts[pair[0]][curValue] = 1;
                                    }
                                } else {
                                    facetCounts[pair[0]][curValue] = 1;
                                }
                            }
                        });
                    });
                }

                _.each(facetCounts, function(facetCount){
                    var values = _.values(facetCount);
                    var totalFacetCount = 0;
                    _.each(values, function(value){
                        totalFacetCount += value;
                    });
                    facetCount["no-value"] = numberOfResults - totalFacetCount;
                });

                this.registerFields(array);
                this.registerFacetCounts(facetCounts,numberOfResults);
            },

            registerFields: function(newFields){
                var that = this;
                _.each(newFields, function(newField){
                    var currentFieldNames = that.fields.pluck('name');
                    if(!_.contains(currentFieldNames, newField.name)){
                        that.fields.add(new Backbone.Model(newField));
                    }
                });
            },

            registerFacetCounts: function(facetCounts, numberOfResults){
                var controller = this;
                var contentTypes = store.get('sources').types();
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

                // set the no value for content type.
                if(!controller.facetCounts[Properties.filters.METADATA_CONTENT_TYPE]['no-value']){
                    var values = _.values(controller.facetCounts[Properties.filters.METADATA_CONTENT_TYPE]);
                    var totalFacetCount = 0;
                    _.each(values, function(value){
                        totalFacetCount += value;
                    });
                    controller.facetCounts[Properties.filters.METADATA_CONTENT_TYPE]["no-value"] = numberOfResults - totalFacetCount;
                }

            }
        });

        return FilterController;
    }
);
