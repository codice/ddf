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
/*global define*/
define([
    'underscore',
    'backbone',
    'js/model/Metacard',
    'js/model/Query'
], function (_, Backbone, Metacard, Query) {

    return new (Backbone.AssociatedModel.extend({
        relations: [
            {
                type: Backbone.One,
                key: 'currentQuery',
                relatedModel: Query.Model
            },
            {
                type: Backbone.One,
                key: 'currentResult',
                relatedModel: Metacard.SearchResult
            },
            {
                type: Backbone.Many,
                key: 'selectedResults',
                relatedModel: Metacard.Metacard
            },
            {
                type: Backbone.Many,
                key: 'activeSearchResults',
                relatedModel: Metacard.MetacardResult
            }
        ],
        defaults: {
            currentQuery: undefined,
            currentResult: undefined,
            selectedResults: [],
            activeSearchResults: [],
            activeSearchResultsAttributes: [],
        },
        initialize: function(){
            this.set('currentResult', new Metacard.SearchResult());
            this.listenTo(this.get('activeSearchResults'), 'update add remove reset', this.updateActiveSearchResultsAttributes);
        },
        updateActiveSearchResultsAttributes: function(){
            var availableAttributes = this.get('activeSearchResults').reduce(function(currentAvailable, result) {
                currentAvailable = _.union(currentAvailable, Object.keys(result.get('metacard').get('properties').toJSON()));
                return currentAvailable;
            }, []).sort();
            this.set('activeSearchResultsAttributes', availableAttributes);
        }, 
        getActiveSearchResultsAttributes: function(){
            return this.get('activeSearchResultsAttributes');
        },
        getActiveSearchResults: function(){
            return this.get('activeSearchResults');
        },
        setActiveSearchResults: function(results){
            this.get('activeSearchResults').reset(results.models || results);
        },
        addToActiveSearchResults: function(results){
            this.get('activeSearchResults').add(results.models || results);
        },
        getSelectedResults: function(){
            return this.get('selectedResults');
        },
        clearSelectedResults: function(){
            this.getSelectedResults().reset();
        },
        addSelectedResult: function(metacard){
            this.getSelectedResults().add(metacard);
        },
        removeSelectedResult: function(metacard){
            this.getSelectedResults().remove(metacard);
        }
    }))();
});