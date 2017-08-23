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

function getMaxIndex(selectionInterface) {
    var selectedResults = selectionInterface.getSelectedResults();
    var completeResults = selectionInterface.getCompleteActiveSearchResults();
    var maxIndex = -1;
    selectedResults.forEach(function (result) {
        maxIndex = Math.max(maxIndex, completeResults.indexOf(result));
    });
    return maxIndex;
}

function getMinIndex(selectionInterface) {
    var selectedResults = selectionInterface.getSelectedResults();
    var completeResults = selectionInterface.getCompleteActiveSearchResults();
    if (selectedResults.length === 0) {
        return -1;
    }
    return selectedResults.reduce(function (minIndex, result) {
        return Math.min(minIndex, completeResults.indexOf(result));
    }, completeResults.length);
}

module.exports = {
    handleClick: function (event) {
        var resultid = event.currentTarget.getAttribute('data-resultid');
        var alreadySelected = this.selectionInterface.getSelectedResults().get(resultid) !== undefined;
        //shift key wins over all else
        if (event.shiftKey) {
            this.handleShiftClick(resultid, alreadySelected);
        } else if (event.ctrlKey || event.metaKey) {
            this.handleControlClick(resultid, alreadySelected);
        } else {
            this.selectionInterface.clearSelectedResults();
            this.handleControlClick(resultid, alreadySelected);
        }
    },
    handleShiftClick: function (resultid, alreadySelected) {
        var selectedResults = this.selectionInterface.getSelectedResults();
        var indexClicked = this.selectionInterface.getCompleteActiveSearchResults().indexOfId(resultid);
        var firstIndex = getMinIndex(this.selectionInterface);
        var lastIndex = getMaxIndex(this.selectionInterface);
        if (firstIndex === -1 && lastIndex === -1) {
            // this.selectionInterface.clearSelectedResults();
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
        this.selectionInterface.addSelectedResult(this.selectionInterface.getCompleteActiveSearchResults().slice(startIndex, endIndex));
    },
    handleControlClick: function (resultid, alreadySelected) {
        if (alreadySelected) {
            this.selectionInterface.removeSelectedResult(this.selectionInterface.getCompleteActiveSearchResults().get(resultid));
        } else {
            this.selectionInterface.addSelectedResult(this.selectionInterface.getCompleteActiveSearchResults().get(resultid));
        }
    }
}