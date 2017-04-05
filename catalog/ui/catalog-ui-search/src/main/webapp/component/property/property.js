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
/*global define*/
define([
    'underscore',
    'backbone'
], function (_, Backbone) {

    return Backbone.Model.extend({
        defaults: {
            value: [],
            values: {},
            enum: undefined,
            label: undefined,
            description: '',
            _initialValue: '',
            readOnly: false,
            validation: '',
            id: '',
            isEditing: false,
            bulk: false,
            multivalued: false,
            type: 'STRING',
            calculatedType: 'text'
        },
        initialize: function(){
            this._setInitialValue();
            this._setCalculatedType();
            this.setDefaultLabel();
        },
        setDefaultLabel: function(){
            if (!this.get('label')){
                this.set('label', this.get('id'));
            }
        },
        getValue: function(){
            return this.get('value');
        },
        setLabel: function(label){
            this.set('label',label);
        },
        setValue: function(val){
            this.set('value', val);
        },
        getId: function(){
            return this.get('id');
        },
        getInitialValue: function(){
            return this.get('_initialValue');
        },
        isReadOnly: function(){
            return this.get('readOnly');
        },
        isEditing: function(){
            return this.get('isEditing');  
        },
        isMultivalued: function(){
            return this.get('multivalued');
        },
        isHomogeneous: function(){
            return !this.get('bulk') || Object.keys(this.get('values')).length <= 1;
        },
        revert: function(){
            this.set('value',this.getInitialValue());
            this.trigger('change:value');
        },
        save: function(value){
            this.set('value', value);
            this.trigger('change:value');
        },
        _setInitialValue: function(){
            this.set('_initialValue', this.getValue());
        },
        _setCalculatedType: function() {
            var calculatedType;

            switch (this.get('type')) {
                case 'DATE':
                    calculatedType = 'date';
                    break;
                case 'BINARY':
                    calculatedType = 'thumbnail';
                    break;
                case 'LOCATION':
                    calculatedType = 'location';
                    break;
                case 'TEXTAREA':
                    calculatedType = 'textarea';
                    break;
                case 'BOOLEAN':
                    calculatedType = 'boolean'
                    break;
                case 'LONG':
                case 'DOUBLE':
                case 'FLOAT':
                case 'INTEGER':
                case 'SHORT':
                    calculatedType = 'number';
                    break;
                case 'RANGE':
                    calculatedType = 'range';
                    break;
                case 'STRING':
                case 'GEOMETRY':
                case 'XML':
                default:
                    calculatedType = 'text';
                    break;
            }
            this.set('calculatedType', calculatedType);
        },
        getCalculatedType: function(){
            return this.get('calculatedType');
        }
    });
});