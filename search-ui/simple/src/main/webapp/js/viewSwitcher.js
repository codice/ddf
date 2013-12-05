/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

/**
 * 
 * This file creates a 'ViewSwitcher' object, which is used to integrate the RecordView
 *  HTML into the Search Page.  
 *  
 *  A ViewSwitcher is created by passing in \<div\> IDs for both the area to display
 *   results and the area to display specific record information.  When showResultsView
 *   or showRecordView is called, the relevant div is populated and shown, and the other
 *   is hidden.
 *   
 *  Additionally, showResultsView creates 'back', 'next', and 'previous' buttons for
 *   navigating between views or records.
 *   
 *  DEPENDENCIES: recordView.js, searchPage.js
 * 
 */


var ViewSwitcher = function(resultsDivId,recordDivId){
	this.resultsDivId = resultsDivId;
	this.recordDivId = recordDivId;
	this.currentView = this.RESULTS_VIEW;
	this.currentIndex = 1;
};

ViewSwitcher.prototype = {
		RESULTS_VIEW : 1,
		METACARD_VIEW : 2,

		getCurrentView : function() {
			return this.currentView;
		},

		getCurrentIndex : function() {
			return this.currentIndex;
		},

		setCurrentIndex : function(index) {
			this.currentIndex = index;
		},

		showResultsView : function(index){
			this.currentView = this.RESULTS_VIEW;
			$("#" + this.recordDivId).hide();
			if(index) {
				loadPageForItem(index);
			}
			$("#" + this.resultsDivId).show();
		},

		showRecordView : function(index) {
			var metacard, previousLi, nextLi, previousA, nextA, recordViewDivId, 
					javascriptPrefix, backA, showResultsHref, showRecordHref, rv;
			
			this.currentView = this.METACARD_VIEW;
			metacard = getMetacard(index);
			if(metacard) {

				$("#" + this.resultsDivId).hide();
				
				// Variables referencing element IDs or var names. Hopefully we find
				//  a way to remove these in the future for better practices.
				previousLi = "#previousRecordLi";
				nextLi = "#nextRecordLi";
				previousA = previousLi+" a";
				nextA = nextLi+" a";
				recordViewDivId = "recordContentDiv";
				backA = "#backToResultsBtn a";
				
				javascriptPrefix = "javascript";
				showResultsHref =  javascriptPrefix + ":viewSwitcher.showResultsView";
				showRecordHref = javascriptPrefix + ":showMetacard";
				
				$(backA).attr("href", showResultsHref + "(" + index + ")");
				
				if(index === 1) {
					$(previousLi).attr("class","disabled");
					$(previousA).removeAttr("href");
				} else {
					$(previousA).attr("href", showRecordHref + "(" + (index - 1) + ")");
					$(previousLi).removeAttr("class");
				}
				if(index === getMaxResults()) {
					$(nextLi).attr("class","disabled");
					$(nextA).removeAttr("href");
				} else {
					$(nextA).attr("href", showRecordHref + "(" + (index + 1) + ")");
					$(nextLi).removeAttr("class");
				}
				
				$("#" + this.recordDivId).show();
				
				rv = new RecordView(metacard);
				rv.buildView(recordViewDivId);
			}
		}
};
