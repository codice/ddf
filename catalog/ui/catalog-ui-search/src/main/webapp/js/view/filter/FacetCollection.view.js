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
    './FacetItem.view',
    'text!templates/filter/facet.collection.handlebars',
    'properties'
], function (_, Marionette, Backbone, FacetItemView, facetCollectionTemplate, Properties) {
    'use strict';
    var FacetCollectionView = Marionette.CompositeView.extend({
        childView: FacetItemView,
        template: facetCollectionTemplate,
        childViewOptions: function () {
            // TODO Hopefully later down the road we can make this more generic instead of hard-coding metadata-content-type
            var queryObject = this.model.parents[0];
            return { isAny: queryObject.filters.getGroupedFilterValues(Properties.filters.METADATA_CONTENT_TYPE).length === 0 };
        },
        childViewContainer: '.facet-items',
        initialize: function (options) {
            if (!this.model.parents || this.model.parents[0] === undefined) {
                return;    // just quit.
            }
            var queryObject = this.model.parents[0];
            var filteredContentTypeIds = queryObject.filters.getGroupedFilterValues(Properties.filters.METADATA_CONTENT_TYPE);
            var facetPairs = _.pairs(options.facetCounts);
            var flattenedFacets = _.map(facetPairs, function (pair) {
                var pairsMapped = _.map(_.pairs(pair[1]), function (innerPair) {
                    return {
                        value: innerPair[0],
                        count: innerPair[1],
                        isInFilter: _.contains(filteredContentTypeIds, innerPair[0])
                    };
                });
                pairsMapped = _.compact(pairsMapped);
                // this will sort by item.value.  no-value will be pushed to the bottom.
                pairsMapped = _.sortBy(pairsMapped, function (item) {
                    if (item.value === 'no-value') {
                        return [
                            2,
                            item.value.toLowerCase()
                        ].join('');
                    }
                    return [
                        0,
                        item.value.toLowerCase()
                    ].join('');
                });
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
