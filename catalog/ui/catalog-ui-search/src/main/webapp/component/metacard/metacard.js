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
    'js/model/Metacard'
], function (_, Backbone, Metacard) {

    return new (Backbone.AssociatedModel.extend({
        relations: [
            {
                type: Backbone.One,
                key: 'currentMetacard',
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
            currentMetacard: undefined,
            selectedResults: [],
            activeSearchResults: []
        },
        initialize: function(){
            this.set('currentResult', new Metacard.SearchResult());
            this.listenTo(this, 'change:currentMetacard', this.handleUpdate);
        },
        handleUpdate: function(){
            this.clearSelectedResults();
            this.setActiveSearchResults(this.get('currentResult').get('results'));
            this.addSelectedResult(this.get('currentMetacard'));
            console.log('update');
        },
        getActiveSearchResults: function(){
            return this.get('activeSearchResults');
        },
        setActiveSearchResults: function(results){
            this.get('activeSearchResults').reset(results.models);
        },
        addToActiveSearchResults: function(results){
            this.get('activeSearchResults').add(results.models);
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