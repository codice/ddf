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
    'backbone',
    '../input'
], function (_, Backbone, Input) {

    var QueryTimeInput = Input.extend({
        defaults: {
            value: '',
            label: '',
            description: '',
            _initialValue: '',
            readOnly: false,
            validation: '',
            id: ''
        },
        initialize: function(){
            this._setInitialValue();
        },
        getValue: function(){
            return this.get('value');
        },
        setLabel: function(label){
            this.set('label',label);
        },
        setValue: function(val){
            this.set('value',value);
        },
        getId: function(){
            return this.get('id');
        },
        _setInitialValue: function(){
            this.set('_initialValue', this.getValue());
        },
        getInitialValue: function(){
            return this.get('_initialValue');
        },
        isReadOnly: function(){
            return this.get('readOnly');
        },
        revert: function(){
            this.set('value',this.getInitialValue());
            this.trigger('change:value');
        },
        save: function(value){
            this.set('value', value);
            this.trigger('change:value');
        },
        type: 'query-time'
    });

    return QueryTimeInput;
});