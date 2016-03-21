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
    'backbone',
    'marionette',
    'properties',
    'js/model/Filter',
    './FilterItem.view',
    'text!templates/filter/filter.collection.handlebars'
], function (Backbone, Marionette, Properties, Filter, FilterItemView, filterCollectionTemplate) {
    'use strict';
    var FilterCollectionView = Marionette.CompositeView.extend({
        template: filterCollectionTemplate,
        childView: FilterItemView,
        className: 'filter-collection-view',
        childViewContainer: '.filter-items',
        collectionEvents: { 'removePressed': 'removePressed' },
        childViewOptions: function () {
            return {
                collection: this.collection,
                fields: this.options.fields
            };
        },
        initialize: function () {
            var view = this;
            if (!this.model.parents || this.model.parents.length === 0) {
                return;    // just quit.  This is an invalid state.
            }
            view.queryObject = this.model.parents[0];
            if (this.queryObject) {
                view.collection = view.queryObject.filters;
            } else {
                return;    // lets just exit.
            }
        },
        removePressed: function (modelToRemove) {
            this.collection.remove(modelToRemove);
        },
        addChild: function (item) {
            var fieldName = item.get('fieldName');
            if (fieldName !== Properties.filters.SOURCE_ID && fieldName !== Properties.filters.METADATA_CONTENT_TYPE) {
                Backbone.Marionette.CollectionView.prototype.addChild.apply(this, arguments);
            }
        }
    });
    return FilterCollectionView;
});
