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
        'underscore',
        'wreqr',
        'backbone',
        'js/model/Query',
        'js/Common',
        'js/ColorGenerator',
        'js/QueryPolling',
        'component/singletons/user-instance',
        'moment',
        'backboneassociations'
    ],
    function ($, _, wreqr, Backbone, Query, Common, ColorGenerator, QueryPolling, user, moment) {

        var Workspace = {};

        Workspace.QueryCollection = Backbone.Collection.extend({
            model: Query.Model,
            initialize: function(){
                var searchList = this;
                this._colorGenerator = ColorGenerator.getNewGenerator();
                this.listenTo(this, 'add', function(query){
                    query.setColor(searchList._colorGenerator.getColor(query.getId()));
                    QueryPolling.handleAddingQuery(query);
                });
                this.listenTo(this, 'remove', function(query){
                    searchList._colorGenerator.removeColor(query.getId());
                    QueryPolling.handleRemovingQuery(query);
                });
            },
            canAddQuery: function(){
                return this.length < 10;
            }
        });

        Workspace.Model = Backbone.AssociatedModel.extend({
            useAjaxSync: true,
            defaults: {
                queries: [],
                metacards: [],
                saved: true
            },
            relations: [
                {
                    type: Backbone.Many,
                    key: 'queries',
                    collectionType: Workspace.QueryCollection
                }
            ],
            canAddQuery: function(){
              return this.get('queries').length < 10;
            },
            addQuery: function () {
                var query = new Query.Model();
                this.get('queries').add(query);
                return query.get('id');
            },
            initialize: function() {
                this.get('queries').on('add',function(){
                    this.trigger('change');
                });
                this.listenTo(this.get('queries'), 'update add remove', this.handleQueryChange);
                this.listenTo(this.get('queries'), 'change', this.handleChange);
                this.listenTo(this, 'change', this.handleChange);
                this.listenTo(this, 'error', this.handleError);
            },
            handleQueryChange: function(){
                this.set('saved', false);
            },
            handleChange: function(model){
                if (model !==undefined &&
                     _.intersection(Object.keys(model.changedAttributes()), [
                         'result', 'saved', 'metacard.modified', 'id', 'subscribed'
                     ]).length === 0){
                    this.set('saved', false);
                }
            },
            saveLocal: function(options){
                this.set('id', this.get('id') || Common.generateUUID());
                var localWorkspaces = this.collection.getLocalWorkspaces();
                localWorkspaces[this.get('id')] = this.toJSON();
                window.localStorage.setItem('workspaces', JSON.stringify(localWorkspaces));
                this.trigger('sync', this, options);
            },
            destroyLocal: function(options){
                var localWorkspaces = this.collection.getLocalWorkspaces();
                delete localWorkspaces[this.get('id')];
                window.localStorage.setItem('workspaces', JSON.stringify(localWorkspaces));
                this.collection.remove(this);
                this.trigger('sync', this, options);
            },
            save: function (options) {
                this.set('saved', true);
                if (this.get('localStorage')) {
                    this.saveLocal(options);
                } else {
                    Backbone.AssociatedModel.prototype.save.apply(this, arguments);
                }
            },
            handleError: function(){
                this.set('saved', false);
            },
            isSaved: function () {
                return this.get('saved');
            },
            destroy: function (options) {
                this.get('queries').forEach(function(query){
                    QueryPolling.handleRemovingQuery(query);
                });
                if (this.get('localStorage')) {
                    this.destroyLocal(options);
                } else {
                    return Backbone.AssociatedModel.prototype.destroy.apply(this, arguments);
                }
            },
            subscribe: function () {
                $.ajax({
                    type: 'post',
                    url: '/intrigue/internal/subscribe/' + this.get('id'),
                }).then(function () {
                    this.set('subscribed', true);
                }.bind(this));
            },
            unsubscribe: function () {
                $.ajax({
                    type: 'post',
                    url: '/intrigue/internal/unsubscribe/' + this.get('id'),
                }).then(function () {
                    this.set('subscribed', false);
                }.bind(this));
            },
            clearResults: function(){
                this.get('queries').forEach(function(queryModel){
                    queryModel.clearResults();
                });
            }
        });

        Workspace.Collection = Backbone.Collection.extend({
            model: Workspace.Model,
            url: '/intrigue/internal/workspaces',
            useAjaxSync: true,
            fetched: false,
            handleSync: function(){
                this.fetched = true;
            },
            initialize: function(){
                this.listenTo(this, 'sync', this.handleSync);
                this.handleUserChange();
                this.listenTo(user, 'change', this.handleUserChange);
                var collection = this;
                collection.on('add',function(workspace){
                    workspace.on('change:lastModifiedDate',function(){
                        collection.sort();
                    });
                });
                wreqr.vent.on('workspace:save', function(){
                    collection.save();
                });
                this.listenTo(this, 'add', this.tagGuestWorkspace);
            },
            handleUserChange: function(){
                this.fetch({remove: false});
            },
            tagGuestWorkspace: function (model) {
                if (this.isGuestUser() && model.isNew()) {
                    model.set({ localStorage: true });
                }
            },
            isGuestUser: function () {
                return user.get('user').isGuestUser();
            },
            comparator: function(workspace){
                return -(moment(workspace.get('lastModifiedDate'))).unix();
            },
            createWorkspace: function(title){
                this.create({title: title || 'New Workspace'});
            },
            createLocalWorkspace: function(){
                var queryForWorkspace = new Query.Model({
                    title: 'Example Local',
                    federation: 'local',
                    cql: "anyText ILIKE '*'"
                });
                this.create({
                    title: 'Template Local',
                    queries: [
                        queryForWorkspace.toJSON()
                    ]
                }).get('queries').first().startSearch();
            },
            createAllWorkspace: function(){
                var queryForWorkspace = new Query.Model({
                    title: 'Example Federated',
                    federation: 'enterprise',
                    cql: "anyText ILIKE '*'"
                });
                this.create({
                    title: 'Template Federated',
                    queries: [
                        queryForWorkspace.toJSON()
                    ]
                }).get('queries').first().startSearch();
            },
            createGeoWorkspace: function(){
                var queryForWorkspace = new Query.Model({
                    title: 'Example Location',
                    cql: "anyText ILIKE '*' AND INTERSECTS(anyGeo, POLYGON((-130.7514 20.6825, -130.7514 44.5780, -65.1230 44.5780, -65.1230 20.6825, -130.7514 20.6825)))"
                });
                this.create({
                    title: 'Template Location',
                    queries: [
                        queryForWorkspace.toJSON()
                    ]
                }).get('queries').first().startSearch();
            },
            createLatestWorkspace: function(){
                var queryForWorkspace = new Query.Model({
                    title: 'Example Temporal',
                    cql: 'anyText ILIKE \'*\' AND ("created" AFTER ' + moment().subtract(1, 'days').toISOString() + ')'
                });
                this.create({
                    title: 'Template Temporal',
                    queries: [
                        queryForWorkspace.toJSON()
                    ]
                }).get('queries').first().startSearch();
            },
            duplicateWorkspace: function(workspace){
                this.create(_.omit(workspace.toJSON(), 'id', 'owner', 'metacard.sharing'));
            },
            saveAll: function(){
                this.forEach(function(workspace){
                    if (!workspace.isSaved()){
                        workspace.save();
                    }
                });
            },
            saveLocalWorkspaces: function () {
                var localWorkspaces = this.chain()
                    .filter(function (workspace) {
                        return workspace.get('localStorage');
                    })
                    .reduce(function (blob, workspace) {
                        blob[workspace.get('id')] = workspace.toJSON();
                        return blob;
                    }, {})
                    .value();

                window.localStorage.setItem('workspaces', JSON.stringify(localWorkspaces));
            },
            convert2_10Format: function(localWorkspaceJSON){
                if (localWorkspaceJSON.constructor === Array){
                    return localWorkspaceJSON.reduce(function(blob, workspace){
                        blob[workspace.id] = workspace;
                        return blob;
                    }, {});
                } else {
                    return localWorkspaceJSON;
                }
            },
            getLocalWorkspaces: function () {
                var localWorkspaces = window.localStorage.getItem('workspaces') || '{}';
                try {
                    return this.convert2_10Format(JSON.parse(localWorkspaces));
                } catch (e) {
                    console.error('Failed to parse local workspaces.', localWorkspaces);
                }
                return {};
            },
            // override parse to merge server response with local storage
            parse: function (resp) {
                var localWorkspaces = _.map(this.getLocalWorkspaces());
                return resp.concat(localWorkspaces);
            }
        });

        return Workspace;

    });
