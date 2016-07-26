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
/*global define,window*/

define([
    'jquery',
    'backbone',
    'poller',
    'underscore',
    'js/model/Workspace',
    'js/model/Selected',
    'component/content/content'
], function ($, Backbone, poller, _, Workspace, Selected, Content) {

    return new (Backbone.Model.extend({
        initialize: function () {
            this.set('content', new Content());
            this.set('workspaces', new Workspace.Collection());

            window.onbeforeunload = function () {
                var unsaved = this.get('workspaces').chain()
                    .map(function (workspace) {
                        if (workspace.dirty()) {
                            return workspace.get('title');
                        }
                    })
                    .filter(function (title) {
                        return title !== undefined;
                    })
                    .value();

                if (unsaved.length > 0) {
                    return 'Do you really want to close? Unsaved workspaces: ' + unsaved.join(', ');
                }
            }.bind(this);

            this.set('selected', new Selected());
            this.listenTo(this.get('workspaces'), 'remove', function(){
                var currentWorkspace = this.getCurrentWorkspace();
                if (currentWorkspace && !this.get("workspaces").get(currentWorkspace)){
                    this.get('content').set('currentWorkspace', undefined);
                }
            });
        },
        getWorkspaceById: function(workspaceId){
            return this.get('workspaces').get(workspaceId);
        },
        setCurrentWorkspaceById: function(workspaceId){
            this.get('content').set('currentWorkspace', this.get('workspaces').get(workspaceId));
        },
        getCurrentWorkspace: function () {
            return this.get('content').get('currentWorkspace');
        },
        getCurrentQueries: function () {
            return this.getCurrentWorkspace().get('queries');
        },
        setQueryById: function (queryId) {
            var queryRef = this.getCurrentQueries().get(queryId);
            this.setQueryByReference(queryRef.clone());
        },
        setQueryByReference: function (queryRef) {
            this.get('content').set('query', queryRef);
        },
        getQuery: function () {
            return this.get('content').get('query');
        },
        getQueryById: function(queryId){
            return this.getCurrentQueries().get(queryId);
        },
        saveQuery: function () {
            var cloneOf = this.getQuery()._cloneOf;
            if (cloneOf === undefined){
                this.addQuery();
                this.setQueryById(this.getQuery().id);
            } else {
                this.updateQuery();
                this.setQueryById(cloneOf);
            }
        },
        resetQuery: function(){
            var cloneOf = this.getQuery()._cloneOf;
            if (cloneOf === undefined){
                this.setQueryByReference(undefined);
            } else {
                this.setQueryById(cloneOf);
            }
        },
        addQuery: function(){
            this.getCurrentQueries().add(this.getQuery());
        },
        updateQuery: function(){
            var query = this.getQuery();
            var newAttributes = _.extend(query.defaults, query.toJSON());
            delete newAttributes.id;
            this.getCurrentQueries().get(query._cloneOf).set(newAttributes);
        },
        getSelectedResults: function(){
            return this.get('content').get('selectedResults');
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
        setActiveSearchResults: function(results){
            this.get('content').setActiveSearchResults(results);
        },
        addToActiveSearchResults: function(results){
            this.get('content').addToActiveSearchResults(results);
        },
        saveCurrentWorkspace: function(){
            this.getCurrentWorkspace().save();
        }
    }))();
});
