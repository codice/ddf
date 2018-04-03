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
    'underscore',
    'jquery',
    './filter.search-form.hbs',
    'js/CustomElements',
    'component/dropdown/filter-comparator/dropdown.filter-comparator.view',
    'component/multivalue/multivalue.view',
    'component/singletons/metacard-definitions',
    'component/property/property',
    'component/dropdown/dropdown',
    'component/dropdown/dropdown.view',
    'component/input/with-param/input-with-param.view',
    'component/value/value',
    'component/filter/filter.view',
    'js/CQLUtils',
    'properties',
    'js/Common'
], function (Marionette, _, $, template, CustomElements, FilterComparatorDropdownView,
             MultivalueView, metacardDefinitions, PropertyModel, DropdownModel, DropdownView,
            InputWithParam, ValueModel, FilterView, CQLUtils, properties, Common) {

    return FilterView.extend({
        template: template,
        className: 'is-search-form',
        initialize: function(){
            FilterView.prototype.initialize.apply(this, arguments);
        },
        onBeforeShow: function(){
            FilterView.prototype.onBeforeShow.call(this);
            this.filterAttribute.currentView.turnOffEditing();
        },
        transformValue: function (value, defaultValue, comparator) {
            switch (comparator) {
                case 'NEAR':
                    if (value[0].constructor !== Object) {
                        value[0] = {
                            value: value[0],
                            distance: 2
                        };
                    }
                    break;
                case 'INTERSECTS':
                case 'DWITHIN':
                    break;
                default:
					if (value[0] != null && value[0].constructor === Object) {
						value[0] = value[0].value;
					} else if(defaultValue != "") {
						value[0] = defaultValue;
					} else if (value == null) {
						value[0] = "";     
					} 
					break;
            }
            return value;
        },
        determineInput: function(){
            this.updateValueFromInput();
            let value = Common.duplicate(this.model.get('value'));
            var defaultValue = "";
            if(typeof this.model.get('defaultValue') !== 'undefined'){
                defaultValue = Common.duplicate(this.model.get('defaultValue'));
            }
            const currentComparator = this.model.get('comparator');
            value = this.transformValue(value, defaultValue, currentComparator);
            FilterView.propertyJSON = FilterView.prototype.generatePropertyJSON(value, this.model.get('type'), currentComparator);
            this.filterInput.show(new MultivalueView({
                model: new PropertyModel(FilterView.propertyJSON)
            }));

            var isEditing = this.$el.hasClass('is-editing');
            if (isEditing){
                this.turnOnEditing();
            } else {
                this.turnOffEditing();
            }
            this.setDefaultComparator(FilterView.propertyJSON);
        },
        turnOnEditing: function(){
            this.$el.addClass('is-editing');
            this.filterComparator.currentView.turnOnEditing();

            var property = this.filterInput.currentView.model instanceof ValueModel
                ? this.filterInput.currentView.model.get('property')
                : this.filterInput.currentView.model;
            property.set('isEditing', true);
        },
        setFilter: function(filter){
            setTimeout(function(){
                if (CQLUtils.isGeoFilter(filter.type)){
                    filter.value = _.clone(filter);
                }
                if (_.isObject(filter.property)) {
                    // if the filter is something like NEAR (which maps to a CQL filter function such as 'proximity'),
                    // there is an enclosing filter that creates the necessary '= TRUE' predicate, and the 'property'
                    // attribute is what actually contains that proximity() call.
                    this.setFilterFromFilterFunction(filter.property);
                } else {
                    this.model.set({
                        value: [filter.value],
                        type: filter.property.split('"').join(''),
                        comparator: this.CQLtoComparator()[filter.type],
                        defaultValue: [filter.defaultValue]
                    });
                }

            }.bind(this),0);
        },
        turnOffEditing: function(){
            this.$el.removeClass('is-editing');
            this.filterComparator.currentView.turnOffEditing();

            var property = this.filterInput.currentView.model instanceof ValueModel
                ? this.filterInput.currentView.model.get('property')
                : this.filterInput.currentView.model;
            property.set('isEditing', false);
        }
    });
});