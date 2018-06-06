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
var template = require('./table-viz.hbs');
var Marionette = require('marionette');
var CustomElements = require('js/CustomElements');
var store = require('js/store');
var $ = require('jquery');
var metacardDefinitions = require('component/singletons/metacard-definitions');
var Common = require('js/Common');
var TableVisibility = require('./table-visibility.view');
var TableRearrange = require('./table-rearrange.view');
var ResultsTableView = require('component/table/results/table-results.view');
var user = require('component/singletons/user-instance');
var properties = require('properties');
var store = require('js/store');

function saveFile (name, type, data) {
    if (data != null && navigator.msSaveBlob)
        return navigator.msSaveBlob(new Blob([data], { type: type }), name);
    var a = $("<a style='display: none;'/>");
    var url = window.URL.createObjectURL(new Blob([data], {type: type}));
    a.attr("href", url);
    a.attr("download", name);
    $("body").append(a);
    a[0].click();
    window.URL.revokeObjectURL(url);
    a.remove();
}

function getFilenameFromContentDisposition(header) {
    if (header == null) {
        return null;
    }

    var parts = header.split('=', 2);
    if (parts.length !== 2) {
        return null;
    }
    //return filename portion
    return parts[1];
}

module.exports = Marionette.LayoutView.extend({
    tagName: CustomElements.register('table-viz'),
    template: template,
    events: {
        'click .options-rearrange': 'startRearrange',
        'click .options-visibility': 'startVisibility',
        'click .options-export-all': 'exportAll',
        'click .options-export-visible': 'exportVisible'
    },
    regions: {
        table: {
            selector: '.tables-container'
        },
        tableVisibility: {
            selector: '.table-visibility',
            replaceElement: true
        },
        tableRearrange: {
            selector: '.table-rearrange',
            replaceElement: true
        }
    },
    initialize: function(options) {
        if (!options.selectionInterface) {
            throw 'Selection interface has not been provided';
        }
        this.listenTo(this.options.selectionInterface, 'reset:activeSearchResults add:activeSearchResults', this.handleEmpty);
    },
    handleEmpty: function() {
        this.$el.toggleClass('is-empty', this.options.selectionInterface.getActiveSearchResults().length === 0);
    },
    onRender: function() {
        this.handleEmpty();
        this.table.show(new ResultsTableView({
            selectionInterface: this.options.selectionInterface
        }));
    },
    startRearrange: function() {
        this.$el.toggleClass('is-rearranging');
        this.tableRearrange.show(new TableRearrange({
            selectionInterface: this.options.selectionInterface
        }), {
            replaceElement: true
        });
    },
    startVisibility: function() {
        this.$el.toggleClass('is-visibilitying');
        this.tableVisibility.show(new TableVisibility({
            selectionInterface: this.options.selectionInterface
        }), {
            replaceElement: true
        });
    },
    saveExport: (data, status, xhr) => {
        var filename = getFilenameFromContentDisposition(xhr.getResponseHeader('Content-Disposition'));
        if (filename === null) {
            filename = 'export' + Date.now() + '.csv';
        }
        saveFile(filename, 'data:attachment/csv', data);
    },
    exportAll: function() {
        let data = {
            hiddenFields: [],
            columnOrder: user.get('user').get('preferences').get('columnOrder'),
            columnAliasMap: properties.attributeAliases,
            metacards: this.options.selectionInterface.getCurrentQuery().get('result').get('results').fullCollection
        };
        $.ajax({
            type: "POST",
            url: './internal/transform/csv?_=' + Date.now(),
            data: JSON.stringify(data),
            contentType: 'application/json',
            success: this.saveExport
        })
    },
    exportVisible: function() {
        let data = {
            applyGlobalHidden: true,
            hiddenFields: user.get('user').get('preferences').get('columnHide'),
            columnOrder: user.get('user').get('preferences').get('columnOrder'),
            columnAliasMap: properties.attributeAliases,
            metacards: this.options.selectionInterface.getActiveSearchResults().toJSON()
        };
        $.ajax({
            type: "POST",
            url: './internal/transform/csv?_=' + Date.now(),
            data: JSON.stringify(data),
            contentType: 'application/json',
            success: this.saveExport
        })
    }
});
