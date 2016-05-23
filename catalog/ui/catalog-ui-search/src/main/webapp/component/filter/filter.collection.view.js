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
/*global define, alert*/
define([
    'marionette',
    'underscore',
    'jquery',
    'js/CustomElements',
    './filter',
    'component/filter-builder/filter-builder'
], function (Marionette, _, $, CustomElements, FilterModel, FilterBuilderModel) {

    return Marionette.CollectionView.extend({
        getChildView: function (item) {
            switch(item.type){
                case 'filter':
                    return this.options['filter-builder'].filterView;
                    break;
                case 'filter-builder':
                    return this.options['filter-builder'].constructor;
                    break;
            }
        },
        tagName: CustomElements.register('filter-collection'),
        addFilter: function(filterModel) {
            filterModel = filterModel || new FilterModel();
            this.collection.add(filterModel);
            return this.children.last();
        },
        addFilterBuilder: function(filterBuilderModel){
            filterBuilderModel = filterBuilderModel || new FilterBuilderModel();
            this.collection.add(filterBuilderModel);
            return this.children.last();
        },
        turnOnEditing: function(){
            this.children.forEach(function(childView){
                childView.turnOnEditing();
            });
        },
        turnOffEditing: function(){
            this.children.forEach(function(childView){
                 childView.turnOffEditing();
            });
        }
    });
});