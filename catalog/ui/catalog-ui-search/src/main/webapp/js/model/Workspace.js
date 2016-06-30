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
        'wreqr',
        'backbone',
        'js/model/Query',
        'js/Common',
        'js/ColorGenerator',
        'js/Common',
        'js/QueryPolling',
        'js/model/user',
        'backboneassociations'
    ],
    function (wreqr, Backbone, Query, Common, ColorGenerator, Common, QueryPolling, user) {

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
            },
            save: function (options) {
                if (this.get('localStorage')) {
                    this.set('id', this.get('id') || Common.generateUUID());
                    this.collection.saveLocal();
                    this.trigger('sync', this, options);
                } else {
                    return Backbone.AssociatedModel.prototype.save.apply(this, arguments);
                }
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
            duplicateWorkspace: function(workspace){
                var workspaceToDuplicate = workspace.toJSON();
                delete workspaceToDuplicate.id;
                this.create(workspaceToDuplicate);
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
            fetch: function (options) {
                options = options || {};
                options.success = function (model) {
                    // merge remote response with local workspaces
                    model.add(JSON.parse(window.localStorage.getItem('workspaces')));
                };
                return Backbone.Collection.prototype.fetch.call(this, options);
            }
        });

        return Workspace;

    });
