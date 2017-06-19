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
/*global require*/


var Marionette = require('marionette');
var InputView = require('../input.view');
var template = require('./input-autocomplete.hbs');
var _ = require('underscore');
require('select2');

module.exports = InputView.extend({
    template,
    onRender() {
        this.initializeSelect();
        InputView.prototype.onRender.call(this);
    },
    initializeSelect(){
        var propertyModel = this.model.get('property');

        var url = propertyModel.get('url');
        var delay = propertyModel.get('delay') || 250;
        var cache = propertyModel.get('cache') || false;
        var placeholder = propertyModel.get('placeholder');
        var minimumInputLength = propertyModel.get('minimumInputLength') || 3;
        var getUrlParams = propertyModel.get('getUrlParams') ||
            function (query) { return { q: query }; };

        var getLabelForResult = propertyModel.get('getLabelForResult') ||
            function (item) { return item.name || item; };

        var getLabelForSelection = propertyModel.get('getLabelForSelection') ||
            getLabelForResult;      

        var processResults = propertyModel.get('processResults') || 
            function (data) {
                var items = data.items;
                if (!Array.isArray(items)) { items = data; }
                if (!Array.isArray(items)) { items = []; }
                return items.map(function(item){
                    return { name: item, id: item };
                });
            };

        this.$el.find('select').select2({
            placeholder,
            minimumInputLength,
            ajax: {
                url, 
                dataType: 'json',
                delay,
                data(params) {
                    return getUrlParams(params.term);
                },
                processResults(data, params) {
                    var results = processResults(data);
                    return {
                        results,
                        pagination: {
                            more: false
                        }
                    };
                },
                cache
            },
            escapeMarkup(markup) { return markup; },
            templateResult(result) {
                if (result.loading) { return result.text; }
                return getLabelForResult(result);
            },
            templateSelection(result) {
                if (!result.id) { return result.text; /* nothing selected */ }
                return getLabelForSelection(result);
            }            
        });
    },
    getCurrentValue(){
        return this.$el.find('select').val();
    },
    onDestroy(){
        this.$el.find('select').select2('destroy');
    }
});