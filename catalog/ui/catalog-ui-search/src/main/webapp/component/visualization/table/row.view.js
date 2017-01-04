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
var wreqr = require('wreqr');
var _ = require('underscore');
var template = require('./row.hbs');
var Marionette = require('marionette');
var CustomElements = require('js/CustomElements');
var store = require('js/store');
var $ = require('jquery');
var metacardDefinitions = require('component/singletons/metacard-definitions');
var Common = require('js/Common');
var user = require('component/singletons/user-instance');
var properties = require('properties');

module.exports = Marionette.ItemView.extend({
    tagName: CustomElements.register('result-row'),
    attributes: function() {
        return {
            'data-resultid': this.model.id
        };
    },
    template: template,
    initialize: function(options) {
        if (!options.selectionInterface) {
            throw 'Selection interface has not been provided';
        }
        this.listenTo(this.model.get('metacard'), 'change:properties', this.render);
        this.listenTo(user.get('user').get('preferences'), 'change:columnHide', this.render);
        this.listenTo(user.get('user').get('preferences'), 'change:columnOrder', this.render);
        this.listenTo(this.options.selectionInterface.getSelectedResults(), 'update add remove reset', this.handleSelectionChange);
        this.handleSelectionChange();
    },
    handleSelectionChange: function() {
        var selectedResults = this.options.selectionInterface.getSelectedResults();
        var isSelected = selectedResults.get(this.model.id);
        this.$el.toggleClass('is-selected', Boolean(isSelected));
    },
    serializeData: function() {
        var prefs = user.get('user').get('preferences');
        var preferredHeader = user.get('user').get('preferences').get('columnOrder');
        var hiddenColumns = user.get('user').get('preferences').get('columnHide');
        var availableAttributes = this.options.selectionInterface.getActiveSearchResultsAttributes();
        var result = this.model.toJSON();
        return {
            id: result.id,
            properties: preferredHeader.filter(function(property) {
                return availableAttributes.indexOf(property) !== -1;
            }).map(function(property) {
                var value = result.metacard.properties[property];
                var text = undefined;
                var html = undefined;
                var className = 'is-text';
                if (value && metacardDefinitions.metacardTypes[property]) {
                    switch (metacardDefinitions.metacardTypes[property].type) {
                        case 'DATE':
                            if (value.constructor === Array) {
                                value = value.map(function(val) {
                                    return Common.getHumanReadableDate(val);
                                });
                            } else {
                                value = Common.getHumanReadableDate(value);
                            }
                            break;
                        default:
                            if (value.constructor === String && value.indexOf('http') === 0) {
                                text = properties.attributeAliases[property] || property;
                                html = '<a href="' + Common.escapeHTML(value) + '" target="_blank">' + text + '</a>';
                            }
                            break;
                    }
                }
                if (property === 'thumbnail') {
                    html = '<img src="' +  Common.escapeHTML(value) + '">';
                    className = "is-thumbnail";
                }
                return {
                    text: text || value,
                    property: property,
                    value: value,
                    html: html,
                    class: className,
                    hidden: hiddenColumns.indexOf(property) >= 0 || properties.isHidden(property) || metacardDefinitions.isHiddenType(property)
                };
            })
        };
    }
});
