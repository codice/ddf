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
/*global define, alert*/
define([
    'marionette',
    'underscore',
    'jquery',
    'js/CustomElements',
    './paging.hbs',
    'lodash/debounce'
], function (Marionette, _, $, CustomElements, template, _debounce) {

    return Marionette.ItemView.extend({
        tagName: CustomElements.register('paging'),
        template: template,
        initialize: function (options) {
            this.listenTo(this.model, 'reset', this.render);
            this.updateSelectionInterface = _debounce(this.updateSelectionInterface, 200, {leading: true, trailing: true});
        },
        updateSelectionInterface: function(){
            this.options.selectionInterface.setActiveSearchResults(this.model.reduce(function(results, result){
                results.push(result);
                if (result.duplicates) {
                    results = results.concat(result.duplicates);
                }
                return results;
            }, []))
        },
        events: {
            'click .first': 'firstPage',
            'click .previous': 'previousPage',
            'click .next': 'nextPage',
            'click .last': 'lastPage'
        },
        firstPage: function() {
            this.model.getFirstPage();
        },
        previousPage: function() {
            this.model.getPreviousPage();
        },
        nextPage: function() {
            this.model.getNextPage();
        },
        lastPage: function() {
            this.model.getLastPage();
        },
        onRender: function(){
            this.updateSelectionInterface();
        },
        serializeData: function(){
            var resultsCollection = this.model;
            return {
                pages: this.currentPages(resultsCollection.state.currentPage, resultsCollection.state.totalPages),
                hasPreviousPage: resultsCollection.hasPreviousPage(),
                hasNextPage: resultsCollection.hasNextPage()
            };
        },
        currentPages: function(current, total){
            var pages = '';
            if (current && total && total > 1) {
                pages = current + ' of ' + total;
            }
            return pages;
        }
    });
});