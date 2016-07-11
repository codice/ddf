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
    'text!./filter.hbs',
    'js/CustomElements',
    'component/dropdown/filter-comparator/dropdown.filter-comparator.view',
    'component/multivalue/multivalue.view',
    'component/singletons/metacard-definitions',
    'component/property/property',
    'component/dropdown/dropdown',
    'component/dropdown/dropdown.view'
], function (Marionette, _, $, template, CustomElements, FilterComparatorDropdownView,
             MultivalueView, metacardDefinitions, PropertyModel, DropdownModel, DropdownView) {

    function isGeoFilter(type){
        return (type === 'DWITHIN' || type === 'INTERSECTS');
    }

    function sanitizeForCql(text){
           return text.split('[').join('(').split(']').join(')').split("'").join('').split('"').join('');
    }

    function bboxToCQLPolygon(model){
        return [
            model.west + ' ' + model.south,
            model.west + ' ' + model.north,
            model.east + ' ' + model.north,
            model.east + ' ' + model.south,
            model.west + ' ' + model.south
        ];
    }

    function polygonToCQLPolygon(model){
        var cqlPolygon = model.map(function(point){
            return point[0] + ' ' + point[1];
        });
        cqlPolygon.push(cqlPolygon[0]);
        return cqlPolygon;
    }

    function generateAnyGeoFilter(property, model){
        switch(model.type){
            case 'POLYGON':
                return {
                    type: 'INTERSECTS',
                    property: property,
                    value: 'POLYGON('+sanitizeForCql(JSON.stringify(polygonToCQLPolygon(model.polygon)))+')'
                };
                break;
            case 'BBOX':
                return {
                    type: 'INTERSECTS',
                    property: property,
                    value: 'POLYGON('+sanitizeForCql(JSON.stringify(bboxToCQLPolygon(model)))+')'
                };
                break;
            case 'POINTRADIUS':
                return {
                    type: 'DWITHIN',
                    property: property,
                    value: 'POINT(' + model.lon + ' ' + model.lat + ')',
                    distance: Number(model.radius)
                };
                break;
        }
    }

    function generateFilter(type, property, value) {
        switch (metacardDefinitions.metacardTypes[property].type) {
            case 'LOCATION':
            case 'GEOMETRY':
                return generateAnyGeoFilter(property, value);
                break;
            default:
                return {
                    type: type,
                    property: '"' + property + '"',
                    value: value
                };
                break;
        }
    }

    var comparatorToCQL = {
        BEFORE: 'BEFORE',
        AFTER: 'AFTER',
        INTERSECTS: 'INTERSECTS',
        CONTAINS: 'ILIKE',
        MATCHCASE: 'LIKE',
        EQUALS: '='
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
                list: metacardDefinitions.sortedMetacardTypes.map(function(metacardType){
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
                default:
                    if (['CONTAINS', 'MATCHCASE', 'EQUALS'].indexOf(currentComparator) === -1) {
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
            text+=this.filterInput.currentView.getCurrentValue();
            text+=')';
            return text;
        },
        getFilters: function(){
            var property = this.model.get('type');
            var type = comparatorToCQL[this.model.get('comparator')];
            if (metacardDefinitions.metacardTypes[this.model.get('type')].multivalued){
                return {
                    type: 'AND',
                    filters: this.filterInput.currentView.getCurrentValue().map(function(currentValue){
                        return generateFilter(type, property, currentValue);
                    })
                }
            } else {
                return generateFilter(type, property, this.filterInput.currentView.getCurrentValue()[0]);
            }
        },
        deleteInvalidFilters: function(){
            var currentValue = this.filterInput.currentView.getCurrentValue()[0];
            if (currentValue === "" || currentValue === null){
                this.delete();
            }
        },
        setFilter: function(filter){
            setTimeout(function(){
                if (isGeoFilter(filter.type)){
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