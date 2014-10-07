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
define([
    'underscore',
    'marionette',
    'backbone',
    'icanhaz',
    './FacetItem.view',
    'text!templates/filter/facet.collection.handlebars',
    'properties'
],
    function (_, Marionette, Backbone, ich, FacetItemView, facetCollectionTemplate, Properties) {
        "use strict";

        ich.addTemplate('facetCollectionTemplate', facetCollectionTemplate);

        var FacetCollectionView = Marionette.CompositeView.extend({
            itemView: FacetItemView,
            template: 'facetCollectionTemplate',
            itemViewContainer: '.facet-items',
            initialize: function(options){

                var queryObject = this.model.parents[0];
                if(queryObject === undefined){
                    return; // just quit.
                }
                var filteredContentTypeIds = queryObject.filters.getGroupedFilterValues(Properties.filters.METADATA_CONTENT_TYPE);
                var facetPairs = _.pairs(options.facetCounts);
                var flattenedFacets = _.map(facetPairs, function(pair){
                    var pairsMapped = _.map(_.pairs(pair[1]), function(innerPair){
                        return {
                            value: innerPair[0],
                            count: innerPair[1],
                            isInFilter: _.contains(filteredContentTypeIds, innerPair[0])
                        };
                    });

                    pairsMapped = _.compact(pairsMapped);

                    return new Backbone.Model({
                        fieldName: pair[0],
                        values: pairsMapped
                    });

                });
                flattenedFacets = _.compact(flattenedFacets);
                this.collection = new Backbone.Collection(flattenedFacets);
            }

        });

        return FacetCollectionView;

    });