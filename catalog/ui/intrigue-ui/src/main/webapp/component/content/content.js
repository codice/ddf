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
    'wreqr',
    'js/model/Metacard',
    'js/model/Query',
    'js/model/Workspace'
], function (_, Backbone, wreqr, Metacard, Query, Workspace) {

    return Backbone.AssociatedModel.extend({
        relations: [
            {
                type: Backbone.One,
                key: 'currentQuery',
                relatedModel: Query.Model
            },
            {
                type: Backbone.One,
                key: 'currentWorkspace',
                relatedModel: Workspace.Model
            },
            {
                type: Backbone.Many,
                key: 'selectedResults',
                relatedModel: Metacard.Metacard
            },
            {
                type: Backbone.Many,
                key: 'results',
                relatedModel: Metacard.Metacard
            },
            {
                type: Backbone.Many,
                key: 'filteredQueries',
                relatedModel: Query.Model
            },
            {
                type: Backbone.Many,
                key: 'activeSearchResults',
                relatedModel: Metacard.MetacardResult
            },
        ],
        defaults: {
            currentWorkspace: undefined,
            selectedResults: [],
            queryId: undefined,
            savedItems: undefined,
            query: undefined,
            state: undefined,
            results: [],  //list of metacards
            filteredQueries: [],
            editing: true,
            activeSearchResults: [],
            activeSearchResultsAttributes: [],
            drawing: false
        },
        initialize: function(){
            this.listenTo(wreqr.vent, 'search:drawline', this.turnOnDrawing);
            this.listenTo(wreqr.vent, 'search:drawcircle', this.turnOnDrawing);
            this.listenTo(wreqr.vent, 'search:drawpoly', this.turnOnDrawing);
            this.listenTo(wreqr.vent, 'search:drawbbox', this.turnOnDrawing);
            this.listenTo(wreqr.vent, 'search:drawstop', this.turnOffDrawing);
            this.listenTo(wreqr.vent, 'search:drawend', this.turnOffDrawing);
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
        turnOnDrawing: function(){
            this.set('drawing', true);
        },
        turnOffDrawing: function(){
            this.set('drawing', false);
        },
        isEditing: function(){
            return this.get('editing');
        },
        turnOnEditing: function(){
            this.set('editing', true);
        },
        turnOffEditing: function(){
            this.set('editing', false);
        },
        getQuery: function(){
            return this.get('query');
        },
        setQuery: function(queryRef){
            this.set('query', queryRef);
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
        filterQuery: function(queryRef) {
            var filteredQueries = this.get('filteredQueries');
            var filtered = Boolean(filteredQueries.get(queryRef));
            if (filtered){
                filteredQueries.remove(queryRef);
            } else {
                filteredQueries.add(queryRef);
            }
        },
        setCurrentQuery: function(query){
            this.set('currentQuery', query);
        },
        getCurrentQuery: function(){
            return this.get('currentQuery');
        }
    });
});