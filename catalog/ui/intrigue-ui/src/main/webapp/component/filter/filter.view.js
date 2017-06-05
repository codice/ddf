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
    './filter.hbs',
    'js/CustomElements',
    'component/dropdown/filter-comparator/dropdown.filter-comparator.view',
    'component/multivalue/multivalue.view',
    'component/singletons/metacard-definitions',
    'component/property/property',
    'component/dropdown/dropdown',
    'component/dropdown/dropdown.view',
    'js/CQLUtils',
    'properties'
], function (Marionette, _, $, template, CustomElements, FilterComparatorDropdownView,
             MultivalueView, metacardDefinitions, PropertyModel, DropdownModel, DropdownView,
            CQLUtils, properties) {

    var comparatorToCQL = {
        BEFORE: 'BEFORE',
        AFTER: 'AFTER',
        INTERSECTS: 'INTERSECTS',
        CONTAINS: 'ILIKE',
        MATCHCASE: 'LIKE',
        EQUALS: '=',
        '>': '>',
        '<': '<',
        '=': '=',
        '<=': '<=',
        '>=': '>=' 
    };

    var CQLtoComparator = {};
    for (var key in comparatorToCQL){
        CQLtoComparator[comparatorToCQL[key]] = key;
    }

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('filter'),
        events: {
            'click > .filter-remove': 'delete'
        },
        modelEvents: {
        },
        regions: {
            filterAttribute: '.filter-attribute',
            filterComparator: '.filter-comparator',
            filterInput: '.filter-input'
        },
        initialize: function(){
            this.listenTo(this.model, 'change:type', this.updateTypeDropdown);
            this.listenTo(this.model, 'change:type', this.determineInput);
            this.listenTo(this.model, 'change:value', this.determineInput);
        },
        onBeforeShow: function(){
            this._filterDropdownModel = new DropdownModel({value: 'CONTAINS'});
            this.filterAttribute.show(DropdownView.createSimpleDropdown({
                list: metacardDefinitions.sortedMetacardTypes.filter(function(metacardType){
                    return !properties.isHidden(metacardType.id);
                }).filter(function(metacardType){
                    return !metacardDefinitions.isHiddenType(metacardType.id);
                }).map(function(metacardType){
                    return {
                        label: metacardType.alias || metacardType.id,
                        value: metacardType.id
                    };
                }),
                defaultSelection: ['anyText'],
                hasFiltering: true
            }));
            this.listenTo(this.filterAttribute.currentView.model, 'change:value', this.handleAttributeUpdate);
            this.filterComparator.show(new FilterComparatorDropdownView({
                model: this._filterDropdownModel,
                modelForComponent: this.model
            }));
            this.determineInput();
        },
        updateTypeDropdown: function(){
            this.filterAttribute.currentView.model.set('value', [this.model.get('type')]);
        },
        handleAttributeUpdate: function(){
            this.model.set('type', this.filterAttribute.currentView.model.get('value')[0]);
        },
        delete: function(){
            this.model.destroy();
        },
        setDefaultComparator: function(propertyJSON){
            var currentComparator = this.model.get('comparator');
            switch(propertyJSON.type){
                case 'LOCATION':
                    if (['INTERSECTS'].indexOf(currentComparator) === -1) {
                        this.model.set('comparator', 'INTERSECTS');
                    }
                    break;
                case 'DATE':
                    if (['BEFORE', 'AFTER'].indexOf(currentComparator) === -1) {
                        this.model.set('comparator', 'BEFORE');
                    }
                    break;
                case 'BOOLEAN': 
                    if (['='].indexOf(currentComparator) === -1){
                        this.model.set('comparator', '=');
                    }
                    break;
                case 'LONG':
                case 'DOUBLE':
                case 'FLOAT':
                case 'INTEGER':
                case 'SHORT':
                    if (['>', '<', '=', '>=', '<='].indexOf(currentComparator) === -1 ){
                        this.model.set('comparator', '>');
                    }
                    break;
                default:
                    if (['CONTAINS', 'MATCHCASE', '='].indexOf(currentComparator) === -1) {
                        this.model.set('comparator', 'CONTAINS');
                    }
                    break;
            }
        },
        determineInput: function(){
            var propertyJSON = _.extend({},
                metacardDefinitions.metacardTypes[this.model.get('type')],
                {
                    value: this.model.get('value'),
                    multivalued: false
                });
            if (propertyJSON.type === 'GEOMETRY'){
                propertyJSON.type = 'LOCATION';
            }
            propertyJSON.placeholder = propertyJSON.type === 'DATE' ? 'DD MMM YYYY HH:mm:ss.SSS' : 'Use * for wildcard.';
            this.filterInput.show(new MultivalueView({
                model: new PropertyModel(propertyJSON)
            }));
            var isEditing = this.$el.hasClass('is-editing');
            if (isEditing){
                this.turnOnEditing();
            } else {
                this.turnOffEditing();
            }
            this.setDefaultComparator(propertyJSON);
           // this.filterInput.currentView.addNewValue();
        },
        getValue: function(){
            var text = '(';
            text+=this.model.get('type') + ' ';
            text+=comparatorToCQL[this.model.get('comparator')] + ' ';
            text+=this.filterInput.currentView.model.getValue();
            text+=')';
            return text;
        },
        getFilters: function(){
            var property = this.model.get('type');
            var type = comparatorToCQL[this.model.get('comparator')];
            if (metacardDefinitions.metacardTypes[this.model.get('type')].multivalued){
                return {
                    type: 'AND',
                    filters: this.filterInput.currentView.model.getValue().map(function(currentValue){
                        return CQLUtils.generateFilter(type, property, currentValue);
                    })
                }
            } else {
                return CQLUtils.generateFilter(type, property, this.filterInput.currentView.model.getValue()[0]);
            }
        },
        deleteInvalidFilters: function(){
            var currentValue = this.filterInput.currentView.model.getValue()[0];
            if (currentValue === null){
                this.delete();
            }
        },
        setFilter: function(filter){
            setTimeout(function(){
                if (CQLUtils.isGeoFilter(filter.type)){
                    filter.value = _.clone(filter);
                }
                this.model.set({
                    value: [filter.value],
                    type: filter.property.split('"').join(''),
                    comparator: CQLtoComparator[filter.type]
                });
            }.bind(this),0);
        },
        onDestroy: function(){
            this._filterDropdownModel.destroy();
        },
        turnOnEditing: function(){
            this.$el.addClass('is-editing');
            this.filterAttribute.currentView.turnOnEditing();
            this.filterComparator.currentView.turnOnEditing();
            this.filterInput.currentView.model.set('isEditing', true);
        },
        turnOffEditing: function(){
            this.$el.removeClass('is-editing');
            this.filterAttribute.currentView.turnOffEditing();
            this.filterComparator.currentView.turnOffEditing();
            this.filterInput.currentView.model.set('isEditing', false);
        }
    });
});