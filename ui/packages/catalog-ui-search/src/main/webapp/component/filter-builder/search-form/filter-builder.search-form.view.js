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
 var Marionette = require('marionette');
 var Backbone = require('backbone');
 var _ = require('underscore');
 var $ = require('jquery');
 var template = require('./filter-builder.search-form.hbs');
 var CustomElements = require('js/CustomElements');
 var FilterBuilderModel = require('component/filter-builder/filter-builder');
 var FilterBuilderView = require('component/filter-builder/filter-builder.view');
 var FilterModel = require('component/filter/filter');
 var FilterView = require('component/filter/search-form/filter.search-form.view');
 var CQLUtils = require('js/CQLUtils');

 module.exports = FilterBuilderView.extend({
    template: template,
    className: 'is-search-form',
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
