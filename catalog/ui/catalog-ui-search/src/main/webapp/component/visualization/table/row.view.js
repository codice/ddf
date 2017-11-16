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
    className: 'is-tr',
    tagName: CustomElements.register('result-row'),
    events: {
        'click .result-download': 'triggerDownload'
    },
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
        this.listenTo(this.model, 'change:metacard>properties change:metacard', this.render);
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
    onRender: function() {
        this.checkIfDownloadable();
        this.$el.attr(this.attributes());
    },
    checkIfDownloadable: function(){
        this.$el.toggleClass('is-downloadable', this.model.get('metacard').get('properties').get('resource-download-url') !== undefined);
    },
    triggerDownload: function(){
        window.open(this.model.get('metacard').get('properties').get('resource-download-url'));
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
                if (value === undefined){
                    value = '';
                }
                if (value.constructor !== Array){
                    value = [value];
                }
                var html;
                var className = 'is-text';
                if (value && metacardDefinitions.metacardTypes[property]) {
                    switch (metacardDefinitions.metacardTypes[property].type) {
                        case 'DATE':
                            value = value.map(function(val) {
                                return val !== undefined && val !== '' ? user.getUserReadableDate(val) : '';
                            });
                            break;
                        default:
                            break;
                    }
                }
                if (property === 'thumbnail') {
                    var escapedValue = Common.escapeHTML(value);
                    html = '<img src="' +  Common.getImageSrc(escapedValue) + '"><button class="is-primary result-download"><span class="fa fa-download"></span></button>';
                    className = "is-thumbnail";
                }
                return {
                    property: property,
                    value: value,
                    html: html,
                    class: className,
                    hidden: hiddenColumns.indexOf(property) >= 0 || properties.isHidden(property) || metacardDefinitions.isHiddenTypeExceptThumbnail(property)
                };
            })
        };
    }
});
