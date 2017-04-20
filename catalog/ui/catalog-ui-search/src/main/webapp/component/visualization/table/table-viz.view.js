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
var MarionetteRegion = require('js/Marionette.Region');
var CustomElements = require('js/CustomElements');
var LoadingCompanionView = require('component/loading-companion/loading-companion.view');
var store = require('js/store');
var $ = require('jquery');
var metacardDefinitions = require('component/singletons/metacard-definitions');
var Common = require('js/Common');
var HeaderView = require('./thead.view');
var BodyView = require('./tbody.view');
var TableVisibility = require('./table-visibility.view');
var TableRearrange = require('./table-rearrange.view');
var ResultsTableView = require('component/table/results/table-results.view');

module.exports = Marionette.LayoutView.extend({
    tagName: CustomElements.register('table-viz'),
    template: template,
    events: {
        'click .options-rearrange': 'startRearrange',
        'click .options-visibility': 'startVisibility'
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
    }
});