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
    'component/dropdown/filter-attribute/dropdown.filter-attribute.view',
    'component/dropdown/filter-comparator/dropdown.filter-comparator.view',
    'component/multivalue/multivalue.view',
    'js/store',
    'component/property/property',
    'component/dropdown/dropdown'
], function (Marionette, _, $, template, CustomElements, FilterAttributeDropdownView, FilterComparatorDropdownView,
             MultivalueView, store, PropertyModel, DropdownModel) {

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
        switch (store.metacardTypes[property].type) {
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
            this.listenTo(this.model, 'change:type', this.determineInput);
            this.listenTo(this.model, 'change:value', this.determineInput);
        },
        onBeforeShow: function(){
            this._attributeDropdownModel = new DropdownModel({value: 'anyText'});
            this._filterDropdownModel = new DropdownModel({value: 'CONTAINS'});
            this.filterAttribute.show(new FilterAttributeDropdownView({
                model: this._attributeDropdownModel,
                modelForComponent: this.model
            }));
            this.filterComparator.show(new FilterComparatorDropdownView({
                model: this._filterDropdownModel,
                modelForComponent: this.model
            }));
            this.determineInput();
        },
        delete: function(){
            this.model.destroy();
        },
        determineInput: function(){
            var propertyJSON = _.extend({},
                store.metacardTypes[this.model.get('type')],
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
            if (store.metacardTypes[this.model.get('type')].multivalued){
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
            this._attributeDropdownModel.destroy();
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