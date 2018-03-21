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
    'js/model/Workspace.collection',
    'component/content/content'
], function ($, Backbone, poller, _, WorkspaceCollection, Content) {

    return new (Backbone.Model.extend({
        initialize: function () {
            this.set('content', new Content());
            this.set('workspaces', new WorkspaceCollection());

            window.onbeforeunload = function () {
                var unsaved = this.get('workspaces').chain()
                    .map(function (workspace) {
                        if (!workspace.isSaved()) {
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

            this.listenTo(this.get('workspaces'), 'remove', function(){
                var currentWorkspace = this.getCurrentWorkspace();
                if (currentWorkspace && !this.get("workspaces").get(currentWorkspace)){
                    this.get('content').set('currentWorkspace', undefined);
                }
            });
            this.listenTo(this.get('content'), 'change:currentWorkspace', this.handleWorkspaceChange);
        },
        clearOtherWorkspaces: function(workspaceId){
            this.get('workspaces').forEach(function(workspaceModel){
                if (workspaceId !== workspaceModel.id){
                    workspaceModel.clearResults();
                }
            });
        },
        handleWorkspaceChange: function(){
            if (this.get('content').changedAttributes().currentWorkspace){
                var previousWorkspace = this.get('content').previousAttributes().currentWorkspace;
                if (previousWorkspace && previousWorkspace.id !== this.get('content').get('currentWorkspace').id){
                    previousWorkspace.clearResults();
                }
            }
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
        getCompleteActiveSearchResultsAttributes: function(){
            return this.get('content').getCompleteActiveSearchResultsAttributes();
        },
        getCompleteActiveSearchResults: function(){
            return this.get('content').getCompleteActiveSearchResults();
        },
        setCompleteActiveSearchResults: function(results){
            this.get('content').setCompleteActiveSearchResults(results);
        },
        getActiveSearchResultsAttributes: function(){
            return this.get('content').getActiveSearchResultsAttributes();
        },
        getActiveSearchResults: function(){
            return this.get('content').getActiveSearchResults();
        },
        setActiveSearchResults: function(results){
            this.get('content').setActiveSearchResults(results);
        },
        addToActiveSearchResults: function(results){
            this.get('content').addToActiveSearchResults(results);
        },
        saveCurrentWorkspace: function(){
            this.getCurrentWorkspace().save();
        },
        setCurrentQuery: function(query){
            this.get('content').setCurrentQuery(query);
        },
        getCurrentQuery: function(){
            return this.get('content').getCurrentQuery();
        }
    }))();
});
