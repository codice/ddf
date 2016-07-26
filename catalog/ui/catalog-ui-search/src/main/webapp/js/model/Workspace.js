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
                metacards: []
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
                this._toJSON = this.toJSON();
            },
            save: function (options) {
                if (this.get('localStorage')) {
                    this.set('id', this.get('id') || Common.generateUUID());
                    this.collection.saveLocal();
                    this.trigger('sync', this, options);
                } else {
                    Backbone.AssociatedModel.prototype.save.apply(this, arguments);
                }
                this._toJSON = this.toJSON();
            },
            dirty: function () {
                return !_.isEqual(this._toJSON, this.toJSON());
            },
            destroy: function (options) {
                this.get('queries').forEach(function(query){
                    QueryPolling.handleRemovingQuery(query);
                });
                if (this.get('localStorage')) {
                    var collection = this.collection;
                    this.collection.remove(this);
                    collection.saveLocal();
                    this.trigger('sync', this, options);
                } else {
                    return Backbone.AssociatedModel.prototype.destroy.apply(this, arguments);
                }
            },
            subscribe: function () {
                $.ajax({
                    type: 'post',
                    url: '/search/catalog/internal/subscribe/' + this.get('id'),
                }).then(function () {
                    this.set('subscribed', true);
                }.bind(this));
            },
            unsubscribe: function () {
                $.ajax({
                    type: 'post',
                    url: '/search/catalog/internal/unsubscribe/' + this.get('id'),
                }).then(function () {
                    this.set('subscribed', false);
                }.bind(this));
            }
        });

        Workspace.Collection = Backbone.Collection.extend({
            model: Workspace.Model,
            url: '/search/catalog/internal/workspaces',
            useAjaxSync: true,
            initialize: function(){
                this.fetch();
                this.listenTo(user, 'change', this.fetch);
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
            tagGuestWorkspace: function (model) {
                if (this.isGuestUser() && model.isNew()) {
                    model.set({ localStorage: true });
                }
            },
            isGuestUser: function () {
                return user.get('user').isGuestUser();
            },
            comparator: function(workspace){
                return -(new Date(workspace.get('lastModifiedDate'))).getTime();
            },
            createWorkspace: function(title){
                this.create({title: title || 'New Workspace'});
            },
            createLocalWorkspace: function(){
                var queryForWorkspace = new Query.Model({
                    title: 'Example Local',
                    federation: 'local',
                    cql: "anyText ILIKE '%'"
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
                    cql: "anyText ILIKE '%'"
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
                    cql: "anyText ILIKE '%' AND INTERSECTS(anyGeo, POLYGON((-130.7514 20.6825, -130.7514 44.5780, -65.1230 44.5780, -65.1230 20.6825, -130.7514 20.6825)))"
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
                    cql: 'anyText ILIKE \'%\' AND ("created" AFTER ' + moment().subtract(1, 'days').toISOString() + ')'
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
            saveLocal: function () {
                var localWorkspaces = this.chain()
                    .filter(function (workspace) {
                        return workspace.get('localStorage');
                    })
                    .map(function (workspace) {
                        return workspace.toJSON();
                    })
                    .value();

                window.localStorage.setItem('workspaces', JSON.stringify(localWorkspaces));
            },
            getLocalWorkspaces: function () {
                var localWorkspaces = window.localStorage.getItem('workspaces') || '[]';
                try {
                    return JSON.parse(localWorkspaces);
                } catch (e) {
                    console.error('Failed to parse local workspaces.', localWorkspaces);
                }
                return [];
            },
            // override parse to merge server response with local storage
            parse: function (resp) {
                return resp.concat(this.getLocalWorkspaces());
            }
        });

        return Workspace;

    });
