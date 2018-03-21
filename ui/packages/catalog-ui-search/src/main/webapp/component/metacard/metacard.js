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
    'js/model/Query',
    'js/model/QueryResponse',
    'js/model/QueryResult'
], function (_, Backbone, Metacard, Query, QueryResponse, QueryResult) {

    return new (Backbone.AssociatedModel.extend({
        relations: [
            {
                type: Backbone.One,
                key: 'currentQuery',
                relatedModel: Query.Model
            },
            {
                type: Backbone.One,
                key: 'currentMetacard',
                relatedModel: QueryResponse
            },
            {
                type: Backbone.Many,
                key: 'selectedResults',
                relatedModel: Metacard
            },
            {
                type: Backbone.Many,
                key: 'activeSearchResults',
                relatedModel: QueryResult
            },
            {
                type: Backbone.Many,
                key: 'completeActiveSearchResults',
                relatedModel: QueryResult
            }
        ],
        defaults: {
            currentQuery: undefined,
            currentMetacard: undefined,
            selectedResults: [],
            activeSearchResults: [],
            activeSearchResultsAttributes: [],
            completeActiveSearchResults: [],
            completeActiveSearchResultsAttributes: [],
        },
        initialize: function(){
            this.set('currentResult', new QueryResponse());
            this.listenTo(this, 'change:currentMetacard', this.handleUpdate);
            this.listenTo(this, 'change:currentMetacard', this.handleCurrentMetacard);
            this.listenTo(this, 'change:currentResult', this.handleResultChange);
            this.listenTo(this.get('activeSearchResults'), 'update add remove reset', this.updateActiveSearchResultsAttributes);
            this.listenTo(this.get('completeActiveSearchResults'), 'update add remove reset', this.updateActiveSearchResultsFullAttributes);
        },
        updateActiveSearchResultsFullAttributes: function() {
            var availableAttributes = this.get('completeActiveSearchResults').reduce(function(currentAvailable, result) {
                currentAvailable = _.union(currentAvailable, Object.keys(result.get('metacard').get('properties').toJSON()));
                return currentAvailable;
            }, []).sort();
            this.set('completeActiveSearchResultsAttributes', availableAttributes);
        },
        getCompleteActiveSearchResultsAttributes: function(){
            return this.get('completeActiveSearchResultsAttributes');
        },
        getCompleteActiveSearchResults: function(){
            return this.get('completeActiveSearchResults');
        },
        setCompleteActiveSearchResults: function(results){
            this.get('completeActiveSearchResults').reset(results.models || results);
        },
        handleResultChange: function(){
            this.listenTo(this.get('currentResult'), 'sync reset:results', this.handleResults);
        },
        handleResults: function(){
            this.set('currentMetacard', this.get('currentResult').get('results').first());
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
        handleUpdate: function(){
            this.clearSelectedResults();
            this.setActiveSearchResults(this.get('currentResult').get('results'));
            this.setCompleteActiveSearchResults(this.get('currentResult').get('results'));
            this.addSelectedResult(this.get('currentMetacard'));
        },
        handleCurrentMetacard: function(){
            if (this.get('currentMetacard') !== undefined){
                this.get('currentQuery').cancelCurrentSearches();
            }
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
        },
        setCurrentQuery: function(query){
            this.set('currentQuery', query);
        },
        getCurrentQuery: function(){
            return this.get('currentQuery');
        }
    }))();
});