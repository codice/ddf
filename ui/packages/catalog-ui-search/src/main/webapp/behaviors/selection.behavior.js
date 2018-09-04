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
const Behaviors = require('./Behaviors');
const Marionette = require('marionette');

function getMaxIndex(selectionInterface) {
    const selectedResults = selectionInterface.getSelectedResults();
    const completeResults = selectionInterface.getCompleteActiveSearchResults();
    return selectedResults.reduce(function (maxIndex, result) {
        return Math.max(maxIndex, completeResults.indexOf(result));
    }, -1);
}

function getMinIndex(selectionInterface) {
    const selectedResults = selectionInterface.getSelectedResults();
    const completeResults = selectionInterface.getCompleteActiveSearchResults();
    return selectedResults.reduce(function (minIndex, result) {
        return Math.min(minIndex, completeResults.indexOf(result));
    }, completeResults.length);
}

const doubleClickTime = 500; // how soon a user has to click for it to be a double click
const textSelectionTime = 200; // how long a user has to hold down mousebutton for us to recognize it as wanting to do text selection

Behaviors.addBehavior('selection', Marionette.Behavior.extend({
    events: function() {
        return {
            [`click ${this.options.selectionSelector}`]: 'handleClick',
            [`mousedown ${this.options.selectionSelector}`]: 'handleMouseDown'
        };   
    },
    lastTarget: undefined,
    lastMouseDown: Date.now(),
    lastClick: Date.now(),
    onRender: function() {
        this.view.$el.addClass('has-selection-behavior');
    },
    updateStateOnClick: function(event) {
        this.lastTarget = event.currentTarget;
        this.lastClick = Date.now();
    },
    handleClick: function(event) {
        this.interpretClick(event);
        this.updateStateOnClick(event);
    },
    interpretClick: function (event) {
        if (event.altKey || this.isTextSelection() || this.isDoubleClick(event)) {
            return;
        }
        const resultid = event.currentTarget.getAttribute('data-resultid');
        const alreadySelected = this.options.selectionInterface.getSelectedResults().get(resultid) !== undefined;
        //shift key wins over all else
        if (event.shiftKey) {
            this.handleShiftClick(resultid, alreadySelected);
        } else if (event.ctrlKey || event.metaKey) {
            this.handleControlClick(resultid, alreadySelected);
        } else {
            this.options.selectionInterface.clearSelectedResults();
            this.handleControlClick(resultid, alreadySelected);
        }
    },
    handleShiftClick: function (resultid, alreadySelected) {
        const selectedResults = this.options.selectionInterface.getSelectedResults();
        const indexClicked = this.options.selectionInterface.getCompleteActiveSearchResults().indexOfId(resultid);
        const firstIndex = getMinIndex(this.options.selectionInterface);
        const lastIndex = getMaxIndex(this.options.selectionInterface);
        if (selectedResults.length === 0) {
            this.handleControlClick(resultid, alreadySelected);
        } else if (indexClicked <= firstIndex) {
            this.selectBetween(indexClicked, firstIndex);
        } else if (indexClicked >= lastIndex) {
            this.selectBetween(lastIndex, indexClicked + 1);
        } else {
            this.selectBetween(firstIndex, indexClicked + 1);
        }
    },
    selectBetween: function (startIndex, endIndex) {
        this.options.selectionInterface.addSelectedResult(this.options.selectionInterface.getCompleteActiveSearchResults().slice(startIndex, endIndex));
    },
    handleControlClick: function (resultid, alreadySelected) {
        if (alreadySelected) {
            this.options.selectionInterface.removeSelectedResult(this.options.selectionInterface.getCompleteActiveSearchResults().get(resultid));
        } else {
            this.options.selectionInterface.addSelectedResult(this.options.selectionInterface.getCompleteActiveSearchResults().get(resultid));
        }
    },
    isDoubleClick: function(event) {
        return this.lastTarget === event.currentTarget && ((Date.now() - this.lastClick) < doubleClickTime);
    },
    isTextSelection: function() {
        return (Date.now() - this.lastMouseDown) > textSelectionTime;
    },
    updateStateOnMouseDown: function() {
        this.lastMouseDown = Date.now();
    },
    handleMouseDown: function(event) {
        this.interpretMouseDown(event);
        this.updateStateOnMouseDown();
    },
    interpretMouseDown: function(event) {
        if (event.altKey) {
            return;
        }
        if (event.ctrlKey || event.metaKey || event.shiftKey || this.isDoubleClick(event)) {
            event.preventDefault();
        }
    }
}));