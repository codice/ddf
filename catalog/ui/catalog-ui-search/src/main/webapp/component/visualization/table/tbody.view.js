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
var $ = require('jquery');
var _ = require('underscore');
var Marionette = require('marionette');
var CustomElements = require('js/CustomElements');
var Common = require('js/Common');
var RowView = require('./row.view');

module.exports = Marionette.CollectionView.extend({
    tagName: CustomElements.register('result-tbody'),
    className: 'is-list has-list-highlighting',
    events: {
        'click > *': 'handleClick',
        'mousedown > *': 'handleMouseDown',
        'click a': 'handleLinkClick'
    },
    childView: RowView,
    childViewOptions: function() {
        return {
            selectionInterface: this.options.selectionInterface
        }
    },
    initialize: function(options) {
        if (!options.selectionInterface) {
            throw 'Selection interface has not been provided';
        }
    },
    handleLinkClick: function(event) {
        event.stopPropagation();
    },
    handleMouseDown: function(event) {
        event.preventDefault();
    },
    handleClick: function(event) {
        var resultItems = this.$el.children();
        var indexClicked = resultItems.index(event.currentTarget);
        var resultid = event.currentTarget.getAttribute('data-resultid');
        var alreadySelected = $(event.currentTarget).hasClass('is-selected');
        //shift key wins over all else
        if (event.shiftKey) {
            this.handleShiftClick(resultid, indexClicked, alreadySelected);
        } else if (event.ctrlKey || event.metaKey) {
            this.handleControlClick(resultid, alreadySelected);
        } else {
            this.options.selectionInterface.clearSelectedResults();
            this.handleControlClick(resultid, alreadySelected);
        }
    },
    handleShiftClick: function(resultid, indexClicked, alreadySelected) {
        var resultItems = this.$el.children();
        var selectedItems = this.$el.children('.is-selected');
        var firstIndex = resultItems.index(selectedItems.first());
        var lastIndex = resultItems.index(selectedItems.last());
        if (firstIndex === -1 && lastIndex === -1) {
            //this.options.selectionInterface.clearSelectedResults();
            this.handleControlClick(resultid, alreadySelected);
        } else if (indexClicked <= firstIndex) {
            this.selectBetween(indexClicked, firstIndex);
        } else if (indexClicked >= lastIndex) {
            this.selectBetween(lastIndex, indexClicked + 1);
        } else {
            this.selectBetween(firstIndex, indexClicked + 1);
        }
    },
    selectBetween: function(startIndex, endIndex) {
        this.options.selectionInterface.addSelectedResult(this.options.selectionInterface.getActiveSearchResults().filter(function(test, index) {
            return (index >= startIndex) && (index < endIndex);
        }));
    },
    handleControlClick: function(resultid, alreadySelected) {
        if (alreadySelected) {
            this.options.selectionInterface.removeSelectedResult(this.options.selectionInterface.getActiveSearchResults().get(resultid));
        } else {
            this.options.selectionInterface.addSelectedResult(this.options.selectionInterface.getActiveSearchResults().get(resultid));
        }
    }
});