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
    'wreqr',
    'properties',
    'js/model/Filter',
    './FacetCollection.view',
    './FilterCollection.view',
    'text!templates/filter/filter.layout.handlebars'
], function (_, Marionette, wreqr, Properties, Filter, FacetCollectionView, FilterCollectionView, filterLayoutTemplate) {
    'use strict';
    var FilterView = Marionette.LayoutView.extend({
        template: filterLayoutTemplate,
        className: 'filter-view',
        events: {
            'click .add-filter': 'addFilterPressed',
            'click .apply': 'refreshSearch',
            'click .filter-status': 'toggleFilterView'
        },
        regions: {
            facetsRegion: '.facets-region',
            filtersRegion: '.filter-region',
            geospatialRegion: '.geospatial-region'
        },
        initialize: function () {
            if (!this.model.parents || this.model.parents.length === 0) {
                return;    // just quit.  This is an invalid state.
            }
            this.queryObject = this.model.parents[0];
            if (this.queryObject) {
                this.collection = this.queryObject.filters;
            } else {
                return;    // lets just exit.
            }
            this.listenTo(wreqr.vent, 'toggleFilterMenu', this.toggleFilterVisibility);
            this.listenTo(wreqr.vent, 'facetSelected', this.addFacet);
            this.listenTo(wreqr.vent, 'facetDeSelected', this.removeFacet);
            wreqr.vent.trigger('processSearch', this.model);
        },
        serializeData: function () {
            return { filterCount: this.queryObject ? this.queryObject.filters.length : 0 };
        },
        onRender: function () {
            var facetCounts = wreqr.reqres.request('getFacetCounts');
            var fields = wreqr.reqres.request('getFields');
            this.facetsRegion.show(new FacetCollectionView({
                model: this.model,
                facetCounts: facetCounts
            }));
            this.filtersRegion.show(new FilterCollectionView({
                model: this.model,
                fields: fields
            }));
            this.initShowFilter();
        },
        initShowFilter: function () {
            var showFilter = wreqr.reqres.request('getShowFilterFlag');
            if (showFilter) {
                this.$el.toggleClass('active', true);
            }
        },
        addFilterPressed: function () {
            var fields = wreqr.reqres.request('getFields');
            var initialSelection = _.first(fields);
            this.collection.add(new Filter.Model({
                fieldName: initialSelection.name,
                fieldType: initialSelection.type,
                fieldOperator: Properties.filters.OPERATIONS[initialSelection.type][0]
            }));
        },
        toggleFilterVisibility: function () {
            this.$el.toggleClass('active');
            wreqr.vent.trigger('filterFlagChanged', this.$el.hasClass('active'));
        },
        toggleFilterView: function () {
            wreqr.vent.trigger('toggleFilterMenu');
        },
        addFacet: function (facet) {
            this.collection.addValueToGroupFilter(facet.fieldName, facet.fieldValue);
        },
        removeFacet: function (facet) {
            this.collection.removeValueFromGroupFilter(facet.fieldName, facet.fieldValue);
        },
        refreshSearch: function () {
            this.collection.trimUnfinishedFilters();
            var progressFunction = function (value, model) {
                model.mergeLatest();
                wreqr.vent.trigger('map:results', model, false);
            };
            if (this.queryObject.get('result') && this.queryObject.get('result').get('status')) {
                var sourceModels = this.collection.where({ fieldName: Properties.filters.SOURCE_ID });
                if (sourceModels.length > 0) {
                    var sources = sourceModels[0].get('stringValue1').split(',');
                    var status = _.reduce(sources, function (memo, src) {
                        memo.push({
                            'id': src,
                            'state': 'ACTIVE'
                        });
                        return memo;
                    }, []);
                    this.queryObject.get('result').get('status').reset(status);
                }
            }
            this.queryObject.startSearch(progressFunction);
        }
    });
    return FilterView;
});
