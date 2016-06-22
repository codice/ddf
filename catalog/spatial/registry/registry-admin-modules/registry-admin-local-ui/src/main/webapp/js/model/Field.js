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
/*global define*/
/*jshint -W024*/
define([
    'backbone',
    'underscore',
    'js/model/FieldDescriptors.js',
    'wreqr',
    'jquery',
    'backboneassociation'


], function (Backbone, _, FieldDescriptors, wreqr) {

    function getSlot(slots, key) {
        for (var index = 0; index < slots.length; index++) {
            if (slots[index].name === key) {
                return slots[index];
            }
        }
    }


    var Field = {};

    Field.FormField = Backbone.AssociatedModel.extend({
        defaults: {
            key: '',
            name: 'unknown',
            desc: 'none',
            type: 'string',  //string,boolean,decimal,integer,data,time,point,bounds,custom
            value: undefined,
            custom: false,
            isSlot: false,
            multiValued: false,
            editable: true,
            required: false

        },
        addValue: function (val) {
            if (!this.get('value')) {
                this.set('value', [val]);
            } else if (_.isArray(this.get('value'))) {
                this.get('value').push(val);
            } else {
                this.set('value', [this.get('value'), val]);
            }
            this.set('value' + (this.get('value').length - 1), val);
        },
        removeValue: function (index) {
            var values = this.get('value');
            for (var count = 0; count < values.length; count++){
                values[count] = this.get('value'+count);
            }

            values.splice(index, 1);
            for (count = 0; count < values.length; count++) {
                this.set('value' + count, values[count]);
            }
            this.unset('value' + values.length);
        },
        validate: function() {
            var error;
            var indices = [];
            if(!this.get('value') && !this.get('regex') && !this.get('required')){
                if(this.hadError){
                    this.hadError = undefined;
                    wreqr.vent.trigger('fieldErrorChange:'+this.get('key'));
                }
                return;
            }

            var regex;
            if(this.get('regex')){
                var flags = this.get('regex').replace(/.*\/([gimy]*)$/, '$1');
                var pattern = this.get('regex').replace(new RegExp('^/(.*?)/'+flags+'$'), '$1');
                regex = new RegExp(pattern, flags);
            }

            if(!this.get('multiValued')) {
                if(this.get('type') === 'number'){
                    if(this.get('min') && this.get('value') < this.get('min')){
                        error = 'Value is less than minimum of ' + this.get('min');
                    } else if (this.get('max') && this.get('value') > this.get('max')) {
                        error =  'Value is greater than maximum of ' + this.get('max');
                    }
                } else if(this.get('type') === 'string' && regex){
                    if(!regex.test(this.get('value'))){
                        error =  this.get('regexMessage') ? this.get('regexMessage') : 'Invalid input value. Must match ' + this.get('regex');
                    }
                } else if(this.get('type') === 'point'){
                    if(this.get('valueLat') && (this.get('valueLat') > 90 || this.get('valueLat') < -90)){
                        error = 'Invalid decimal latitude value. Must be -90 < lat < 90';
                    } else if(this.get('valueLon') && (this.get('valueLon') > 180 || this.get('valueLon')< -180)){
                        error = 'Invalid decimal longitude value. Must be -180 < lat < 180';
                    }
                }
            } else {
                if(this.get('type') === 'string' && regex) {
                    for (var index = 0; index < this.get('value').length; index++) {
                        if(this.get('value' + index)) {
                            if (!regex.test(this.get('value' + index))) {
                                indices.push(index);
                                error = this.get('regexMessage') ? this.get('regexMessage') : 'Invalid input value. Must match ' + this.get('regex');
                            }
                        }
                    }
                }
            }

            if (!error && (!this.get('value') || (_.isArray(this.get('value')) && this.get('value').length === 0) ) && this.get('required')) {
                error = 'Required Field';
            }

            this.errorIndices = indices;
            if((!this.hadError && error) || (!error && this.hadError)){
                this.hadError = error;
                wreqr.vent.trigger('fieldErrorChange:'+this.get('key'));
            }
            wreqr.vent.trigger('fieldChanged:'+this.get('parentId'));
            return error;
        },
        saveData: function(backingData, ebrimTypes) {
            if(this.validationError){
                return this.validationError;
            }
            if (!this.get('isSlot')) {
                if (this.get('value') && !(_.isArray(this.get('value')) && this.get('value').length === 0)) {
                    backingData[this.get('key')] = this.get('value');
                } else if( backingData[this.get('key')]){
                    delete backingData[this.get('key')];
                }

            } else {
                var slot = getSlot(backingData.Slot, this.get('key'));
                var newSlot = false;
                if (!slot) {
                    slot = {
                        slotType: ebrimTypes[this.get('type')],
                        name: this.get('key')
                    };
                    newSlot = true;
                }
                slot.value = undefined;

                if (this.get('type') === 'date') {
                    if (this.get('valueDate')) {
                        var time = '00:00';
                        if (this.get('valueTime')) {
                            time = this.get('valueTime');
                        }
                        slot.value = [this.get('valueDate') + 'T' + time + 'Z'];
                    }
                } else if (this.get('type') === 'point') {
                    if (this.get('valueLat') && this.get('valueLon')) {
                        slot.value = {
                            Point: {
                                srsName: 'urn:ogc:def:crs:EPSG::4326',
                                srsDimension: 2,
                                pos:  this.get('valueLon') + ' ' +this.get('valueLat') 
                            }
                        };
                    }
                } else if (this.get('type') === 'boolean') {
                    slot.value = this.get('value') ? 'true' : 'false';
                } else if (this.get('multiValued')) {
                    var values = [];
                    for (var index = 0; index < this.get('value').length; index++) {
                        if(this.get('value' + index)) {
                            values.push(this.get('value' + index));
                        }
                    }
                    slot.value = values.length > 0 ? values : undefined;
                } else {
                    if (this.get('value')) {
                        slot.value = this.get('value');
                    }
                }

                if (newSlot && slot.value) {
                    backingData.Slot.push(slot);
                }

                if (!newSlot && !slot.value) {
                    var slotIndex = backingData.Slot.map(function (e) {
                        return e.name;
                    }).indexOf(slot.name);
                    backingData.Slot.splice(slotIndex, 1);
                }
            }
        }
    });

    Field.FormFields = Backbone.Collection.extend({
        model: Field.FormField
    });

    return Field;
});