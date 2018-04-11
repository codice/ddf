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
        transformValue: function (value, comparator) {
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
                   if (value[0] == null) {
                        value[0] = "";
                   } else if (value[0].constructor === Object) {
                        value[0] = value[0].value;
                   }
                   break;
            }
            return value;
        },
        turnOnEditing: function(){
            this.$el.addClass('is-editing');
            this.filterComparator.currentView.turnOnEditing();

            var property = this.filterInput.currentView.model instanceof ValueModel
                ? this.filterInput.currentView.model.get('property')
                : this.filterInput.currentView.model;
            property.set('isEditing', true);
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