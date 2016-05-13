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
    'backbone',
    'poller',
    'underscore',
    'js/model/Workspace',
    'js/model/source',
    'component/workspaces/workspaces',
    'js/model/Selected',
    'component/content/content',
    'component/router/router'
], function (Backbone, poller, _, Workspace, Source, Workspaces, Selected, Content, Router) {

    return new (Backbone.Model.extend({
        defaults: {
            initialized: false
        },
        setupListeners: function(model, listeners){
            if (listeners !== undefined){
                this.listenTo(model, listeners);
            }
        },
        setupPolling: function(model, opts){
            if (opts.persisted){
                model.fetch();
                if (opts.poll) {
                    poller.get(model, opts.poll).start();
                }
            }
        },
        initModel: function (Model, opts) {
            opts = _.extend({
                persisted: true,
                poll: false
            }, opts);
            var model = new Model();
            this.setupListeners(model, opts.listeners);
            this.setupPolling(model, opts);
            return model;
        },
        initialize: function () {
            this.set('content', this.initModel(Content, {
                persisted: false,
                listeners: {
                    'change:currentWorkspace': this.clearResults
                }
            }));
            this.set('workspaces', this.initModel(Workspace.Collection, {
                listeners: {
                    'sync': this.handleWorkspaceSync
                }
            }));
            this.set('sources', Source);
            this.set('componentWorkspaces', this.initModel(Workspaces, {
                persisted: false
            }));
            this.set('selected', this.initModel(Selected, {
                persisted: false
            }));
            this.set('router', this.initModel(Router, {
                persisted: false
            }));
        },
        handleWorkspaceDestruction: function(model, workspaceCollection){
            if (workspaceCollection.length === 0){
                console.log('creating a workspace for you');
                this.get('content').set('currentWorkspace', this.get('workspaces').createWorkspace('My First Workspace'));
            }
            if (this.get('content').attributes.currentWorkspace === model) {
                this.get('content').set('currentWorkspace', workspaceCollection.first());
            }
        },
        handleWorkspaceSync: function(workspaceCollection){
            this.set('initialized', true);
            if (this.get('content').get('currentWorkspace') === undefined){
                if (workspaceCollection.length === undefined){
                    this.get('content').set('currentWorkspace', workspaceCollection);
                } else if (workspaceCollection.length > 0) {
                    this.get('content').set('currentWorkspace', workspaceCollection.first());
                } else {
                    console.log('creating a workspace for you');
                    this.get('content').set('currentWorkspace', this.get('workspaces').createWorkspace('My First Workspace'));
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
        saveQuery: function () {
            var cloneOf = this.getQuery()._cloneOf;
            if (cloneOf === undefined){
                this.addQuery();
                this.setQueryById(this.getQuery().cid);
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
        clearResults: function(){
            this.get('content').get('results').reset();
        },
        getFilteredQueries: function(){
            return this.get('content').get('filteredQueries');
        },
        filterQuery: function(queryId){
            this.get('content').filterQuery(this.getQueryById(queryId));
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
        addMetacardTypes: function(metacardTypes){
            this.get('content').addMetacardTypes(metacardTypes);
        },
        saveCurrentSelection: function(){
            var selectedResults = this.getSelectedResults().pluck('id');
            var savedMetacards = _.union(this.getCurrentWorkspace().get('metacards'), selectedResults);
            this.getCurrentWorkspace().set('metacards', savedMetacards);
        },
        saveCurrentWorkspace: function(){
            this.getCurrentWorkspace().save();
        },
        deleteCurrentWorkspace: function(){
            this.getCurrentWorkspace().destroy();
        }
    }))();
});
