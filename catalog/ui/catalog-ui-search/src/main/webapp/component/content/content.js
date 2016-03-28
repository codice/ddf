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

    var Content = Backbone.AssociatedModel.extend({
        relations: [
            {
                type: Backbone.Many,
                key: 'results',
                relatedModel: Metacard.Metacard
            },
            {
                type: Backbone.Many,
                key: 'filteredQueries',
                relatedModel: Query.Model
            }
        ],
        defaults: {
            queryId: undefined,
            savedItems: undefined,
            query: undefined,
            state: undefined,
            results: [],  //list of metacards
            filteredQueries: [],
            editing: true
        },
        initialize: function(){
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
        filterQuery: function(queryRef) {
            var filteredQueries = this.get('filteredQueries');
            var filtered = Boolean(filteredQueries.get(queryRef));
            if (filtered){
                filteredQueries.remove(queryRef);
            } else {
                filteredQueries.add(queryRef);
            }
        }
    });

    return Content;
});