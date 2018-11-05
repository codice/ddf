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
    'component/relative-time/relative-time.view',
    'component/value/value',
    'js/CQLUtils',
    'properties',
    'js/Common'
], function (Marionette, _, $, template, CustomElements, FilterComparatorDropdownView,
             MultivalueView, metacardDefinitions, PropertyModel, DropdownModel, DropdownView,
            RelativeTimeView, ValueModel, CQLUtils, properties, Common) {

    const generatePropertyJSON = (value, type, comparator) => {
        const propertyJSON = _.extend({},
            metacardDefinitions.metacardTypes[type],
            {
                value: value,
                multivalued: false,
                enumFiltering: true,
                enumCustom: true,
                matchcase: ['MATCHCASE', '='].indexOf(comparator) !== -1 ? true : false,
                enum: metacardDefinitions.enums[type],
                showValidationIssues: false
            });
            
        if (propertyJSON.type === 'GEOMETRY'){
            propertyJSON.type = 'LOCATION';
        }

        propertyJSON.placeholder = propertyJSON.type === 'DATE' ? 'DD MMM YYYY HH:mm:ss.SSS' : 'Use * for wildcard.';

        if (comparator === 'NEAR') {
            propertyJSON.type = 'NEAR';
            propertyJSON.param = 'within';
            propertyJSON.help =  'The distance (number of words) within which search terms must be found in order to match';
            delete propertyJSON.enum;
        }

        // if we don't set this the property model will transform the value as if it's a date, clobbering the duration format
        if (comparator === 'RELATIVE') {
            propertyJSON.transformValue = false;
        }

        return propertyJSON;
    }

    // Relative time view is needed for the relative date, multivalue for all else
    const determineView = (comparator) => {
        let necessaryView;
        switch(comparator) {
            case 'RELATIVE':
                necessaryView = RelativeTimeView;
            break;
            default:
                necessaryView = MultivalueView;
            break;
        }
        return necessaryView;
    }

    // strip extra quotes
    const stripQuotes = (property) => {
        return property.replace(/^"(.+(?="$))"$/, '$1');
    }

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('filter'),
        attributes: function() {
            return { 'data-id': this.model.cid }
        },
        events: {
            'click > .filter-remove': 'delete'
        },
        modelEvents: {
        },
        regions: {
            filterRearrange: '.filter-rearrange',
            filterAttribute: '.filter-attribute',
            filterComparator: '.filter-comparator',
            filterInput: '.filter-input'
        },
        initialize: function(){
            this.listenTo(this.model, 'change:type', this.updateTypeDropdown);
            this.listenTo(this.model, 'change:type', this.determineInput);
            this.listenTo(this.model, 'change:value', this.determineInput);
            this.listenTo(this.model, 'change:comparator', this.determineInput);
        },
        onBeforeShow: function(){
            this.$el.toggleClass('is-sortable', this.options.isSortable || true);
            this._filterDropdownModel = new DropdownModel({value: 'CONTAINS'});
            this.filterAttribute.show(DropdownView.createSimpleDropdown({
                list: metacardDefinitions.sortedMetacardTypes.filter(function(metacardType){
                    return !properties.isHidden(metacardType.id);
                }).filter(function(metacardType){
                    return !metacardDefinitions.isHiddenType(metacardType.id);
                }).map(function(metacardType){
                    return {
                        label: metacardType.alias || metacardType.id,
                        description: (properties.attributeDescriptions || {})[metacardType.id],
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
                    if (value === null || value[0] === null) {
                        value = [""];
                        break;
                    }
                    if (value[0].constructor === Object) {
                        value[0] = value[0].value;
                    }
                    break;
            }
            return value;
        },
        comparatorToCQL: function() {
            return {
                BEFORE: 'BEFORE',
                AFTER: 'AFTER',
                RELATIVE: '=',
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
        },
        CQLtoComparator: function() {
            var comparator = {};
            for (var key in this.comparatorToCQL()){
                comparator[this.comparatorToCQL()[key]] = key;
            }
            return comparator;
        },
        // With the relative date comparator being the same as =, we need to try and differentiate them this way
        getComparatorForFilter(filter) {
            const propertyDefinition = metacardDefinitions.metacardTypes[stripQuotes(filter.property)];
            if (propertyDefinition && propertyDefinition.type === 'DATE' && filter.type === '=') {
                return 'RELATIVE';
            } else {
                return this.CQLtoComparator()[filter.type];
            }
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
        toggleLocationClass: function(toggle){
            this.$el.toggleClass('is-location', toggle);
        },
        setDefaultComparator: function(propertyJSON){
            this.toggleLocationClass(false);
            var currentComparator = this.model.get('comparator');
            switch(propertyJSON.type){
                case 'LOCATION':
                    if (['INTERSECTS'].indexOf(currentComparator) === -1) {
                        this.model.set('comparator', 'INTERSECTS');
                    }
                    this.toggleLocationClass(true);
                    break;
                case 'DATE':
                    if (['BEFORE', 'AFTER', 'RELATIVE'].indexOf(currentComparator) === -1) {
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
                    if (['CONTAINS', 'MATCHCASE', '=', 'NEAR'].indexOf(currentComparator) === -1) {
                        this.model.set('comparator', 'CONTAINS');
                    }
                    break;
            }
        },
        updateValueFromInput: function() {
            if (this.filterInput.currentView && this.filterInput.currentView.model.hasChanged()) {
                this.model.set('value', Common.duplicate(this.filterInput.currentView.model.getValue()));
            }
        },
        determineInput: function(){
            this.updateValueFromInput();
            let value = Common.duplicate(this.model.get('value'));
            const currentComparator = this.model.get('comparator');
            value = this.transformValue(value, currentComparator);
            const propertyJSON = generatePropertyJSON(value, this.model.get('type'), currentComparator);
            const ViewToUse = determineView(currentComparator);
            this.filterInput.show(new ViewToUse({
                model: new PropertyModel(propertyJSON)
            }));

            var isEditing = this.$el.hasClass('is-editing');
            if (isEditing){
                this.turnOnEditing();
            } else {
                this.turnOffEditing();
            }
            this.setDefaultComparator(propertyJSON);
        },
        getValue: function(){
            var text = '(';
            text+=this.model.get('type') + ' ';
            text+=this.comparatorToCQL()[this.model.get('comparator')] + ' ';
            text+=this.filterInput.currentView.model.getValue();
            text+=')';
            return text;
        },
        getFilters: function(){
            var property = this.model.get('type');
            var comparator = this.model.get('comparator');
            var value = this.filterInput.currentView.model.getValue()[0];

            if (comparator === 'NEAR') {
                return CQLUtils.generateFilterForFilterFunction(
                    'proximity',
                    [property, value.distance, value.value]
                );
            }

            var type = this.comparatorToCQL()[comparator];
            if (metacardDefinitions.metacardTypes[this.model.get('type')].multivalued){
                return {
                    type: 'AND',
                    filters: this.filterInput.currentView.model.getValue().map(function(currentValue){
                        return CQLUtils.generateFilter(type, property, currentValue);
                    })
                }
            } else {
                return CQLUtils.generateFilter(type, property, value);
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
                if (_.isObject(filter.property)) {
                    // if the filter is something like NEAR (which maps to a CQL filter function such as 'proximity'),
                    // there is an enclosing filter that creates the necessary '= TRUE' predicate, and the 'property'
                    // attribute is what actually contains that proximity() call.
                    this.setFilterFromFilterFunction(filter.property);
                } else {
                    this.model.set({
                        value: [filter.value],
                        type: filter.property.split('"').join(''),
                        comparator: this.getComparatorForFilter(filter)
                    });
                }

            }.bind(this),0);
        },
        setFilterFromFilterFunction(filter) {
            if (filter.filterFunctionName === 'proximity') {
                var property = filter.params[0];
                var distance = filter.params[1];
                var value = filter.params[2];

                this.model.set({
                    value: [{
                        value: value,
                        distance: distance
                    }],
                    // this is confusing but 'type' on the model is actually the name of the property we're filtering on
                    type: property,
                    comparator: 'NEAR'
                });
            } else {
                throw new Error('Unsupported filter function in filter view: ' + filterFunctionName);
            }
        },
        onDestroy: function(){
            this._filterDropdownModel.destroy();
        },
        turnOnEditing: function(){
            this.$el.addClass('is-editing');
            this.filterAttribute.currentView.turnOnEditing();
            this.filterComparator.currentView.turnOnEditing();

            var property = this.filterInput.currentView.model instanceof ValueModel
                ? this.filterInput.currentView.model.get('property')
                : this.filterInput.currentView.model;
            property.set('isEditing', true);
        },
        turnOffEditing: function(){
            this.$el.removeClass('is-editing');
            this.filterAttribute.currentView.turnOffEditing();
            this.filterComparator.currentView.turnOffEditing();

            var property = this.filterInput.currentView.model instanceof ValueModel
                ? this.filterInput.currentView.model.get('property')
                : this.filterInput.currentView.model;
            property.set('isEditing', this.options.isForm === true || this.options.isFormBuilder === true);
        }
    });
});