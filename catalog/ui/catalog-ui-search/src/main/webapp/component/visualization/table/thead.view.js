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
var $ = require('jquery');
var template = require('./thead.hbs');
var Marionette = require('marionette');
var CustomElements = require('js/CustomElements');
var Common = require('js/Common');
var user = require('component/singletons/user-instance');
var properties = require('properties');
var metacardDefinitions = require('component/singletons/metacard-definitions');
var blacklist = ['metacard-type', 'source-id', 'cached', 'metacard-tags', 'anyText'];

module.exports = Marionette.ItemView.extend({
    template: template,
    tagName: CustomElements.register('result-thead'),
    events: {
        'click th.is-sortable': 'updateSorting'
    },
    initialize: function(options) {
        if (!options.selectionInterface) {
            throw 'Selection interface has not been provided';
        }
        this.listenTo(this.options.selectionInterface, 'reset:activeSearchResults add:activeSearchResults', this.render);
        this.listenTo(user.get('user').get('preferences'), 'change:columnHide', this.render);
        this.listenTo(user.get('user').get('preferences'), 'change:columnOrder', this.render);
    },
    onRender: function() {
        this.handleSorting();
    },
    updateSorting: function(e) {
        var attribute = e.currentTarget.getAttribute('data-propertyid');
        var $currentTarget = $(e.currentTarget);
        var direction = $currentTarget.hasClass('is-sorted-asc') ? 'descending' : 'ascending';
        var sort = [{
            attribute: attribute,
            direction: direction
        }];
        var prefs = user.get('user').get('preferences');
        prefs.set('resultSort', sort);
        prefs.savePreferences();
    },
    handleSorting: function() {
        var resultSort = user.get('user').get('preferences').get('resultSort');
        this.$el.children('.is-sorted-asc').removeClass('is-sorted-asc');
        this.$el.children('.is-sorted-desc').removeClass('is-sorted-desc');
        if (resultSort) {
            resultSort.forEach(function(sort) {
                switch (sort.direction) {
                    case 'ascending':
                        this.$el.children('[data-propertyid="' + sort.attribute + '"]').addClass('is-sorted-asc');
                        break;
                    case 'descending':
                        this.$el.children('[data-propertyid="' + sort.attribute + '"]').addClass('is-sorted-desc');
                        break;
                    default:
                        break;
                }
            }.bind(this));
        }
    },
    serializeData: function() {
        var sortAttributes = _.filter(metacardDefinitions.sortedMetacardTypes, function(type) {
            return type.type === 'STRING' || type.type === 'DATE';
        }).filter(function(type) {
            return blacklist.indexOf(type.id) === -1;
        }).map(function(type) {
            return type.id;
        });
        var prefs = user.get('user').get('preferences');
        var results = this.options.selectionInterface.getActiveSearchResults().toJSON();
        var preferredHeader = user.get('user').get('preferences').get('columnOrder');
        var hiddenColumns = user.get('user').get('preferences').get('columnHide');
        var availableAttributes = this.options.selectionInterface.getActiveSearchResultsAttributes();

        // tack on unknown attributes to end (sorted), then save
        preferredHeader = _.union(preferredHeader, availableAttributes);
        prefs.set('columnOrder', preferredHeader);
        prefs.savePreferences();

        return preferredHeader.filter(function(property) {
            return availableAttributes.indexOf(property) !== -1;
        }).map(function(property) {
            return {
                label: properties.attributeAliases[property],
                id: property,
                hidden: hiddenColumns.indexOf(property) >= 0,
                sortable: sortAttributes.indexOf(property) >= 0
            };
        });
    }
});