/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define, alert, setTimeout*/
define([
    'marionette',
    'backbone',
    'underscore',
    'jquery',
    './filter-builder.hbs',
    'js/CustomElements',
    'component/filter-builder/filter-builder',
    'component/filter-builder/filter-builder.view',
    'component/filter/filter',
    'component/filter-search-form/filter.view',
    'js/CQLUtils'
], function (Marionette, Backbone, _, $, template, CustomElements, FilterBuilderModel, FilterBuilderView, FilterModel, FilterView, CQLUtils) {

    return FilterBuilderView.extend({
        template: template,
        tagName: CustomElements.register('filter-builder-search-form'),
        initialize: function(){
            FilterBuilderView.prototype.initialize.apply(this, arguments);
        },
        onBeforeShow: function(){
            FilterBuilderView.prototype.onBeforeShow.call(this);
            this.filterOperator.currentView.turnOffEditing();
        },
        addFilterBuilder: function(){
            var FilterBuilderView = this.filterContents.currentView.addFilterBuilder(new FilterBuilderModel());
            this.handleEditing();
            return FilterBuilderView;
        },
        transformToCql: function(){
            var filter = this.getFilters();
            if (filter.filters.length === 0){
                return "(\"anyText\" ILIKE '*')";
            } else {
                return CQLUtils.transformFilterToCQL(filter);
            }
        },
        turnOnEditing: function(){
            this.$el.addClass('is-editing');
            this.filterOperator.currentView.turnOffEditing();
            this.filterContents.currentView.turnOnEditing();
        },
        createFilterModel: function() {
            return new FilterModel({
                isResultFilter: Boolean(this.model.get("isResultFilter"))
            });
        },
        addFilter: function() {
            var FilterView = this.filterContents.currentView.addFilter(this.createFilterModel());
            this.handleEditing();
            return FilterView;
        },
        filterView: FilterView
    });
});
