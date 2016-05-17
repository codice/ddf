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
/*global define,window,setTimeout*/

define([
        'wreqr',
        'backbone',
        'js/model/Query',
        'js/Common',
        'js/ColorGenerator',
        'js/Common',
        'backboneassociations'
    ],
    function (wreqr, Backbone, Query, Common, ColorGenerator, Common) {
        var Workspace = {};

        Workspace.QueryCollection = Backbone.Collection.extend({
            model: Query.Model,
            initialize: function(){
                var searchList = this;
                this._colorGenerator = ColorGenerator.getNewGenerator();
                this.listenTo(this, 'add', function(query){
                    query.setColor(searchList._colorGenerator.getColor(query.getId()));
                });
                this.listenTo(this, 'remove', function(query){
                    searchList._colorGenerator.removeColor(query.getId());
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
                if (this.collection.isGuestUser()) {
                    this.set('id', this.get('id') || Common.generateUUID());
                    this.collection.save();
                    this.trigger('sync', this, options);
                } else {
                    return Backbone.AssociatedModel.prototype.save.apply(this, arguments);
                }
            },
            destroy: function (options) {
                if (this.collection.isGuestUser()) {
                    var collection = this.collection;
                    this.collection.remove(this);
                    collection.save();
                    this.trigger('sync', this, options);
                } else {
                    return Backbone.AssociatedModel.prototype.destroy.apply(this, arguments);
                }
            }
        });

        Workspace.Collection = Backbone.Collection.extend({
            model: Workspace.Model,
            url: '/services/search/catalog/workspaces',
            useAjaxSync: true,
            initialize: function(options){
                this.store = options.store;
                this.listenTo(this.store.get('user'), 'change', this.fetch);
                var collection = this;
                collection.on('add',function(workspace){
                    workspace.on('change:lastModifiedDate',function(){
                        collection.sort();
                    });
                });
                wreqr.vent.on('workspace:save', function(){
                    collection.save();
                });
            },
            isGuestUser: function () {
                return this.store.get('user').get('user').isGuestUser();
            },
            comparator: function(workspace){
                return -(new Date(workspace.get('lastModifiedDate'))).getTime();
            },
            createWorkspace: function(title){
               this.create({title: title || 'New Workspace'});
            },
            save: function () {
                window.localStorage.setItem('workspaces', JSON.stringify(this.toJSON()));
            },
            fetch: function (options) {
                if (this.isGuestUser()) {
                    setTimeout(function () {
                        this.set(JSON.parse(window.localStorage.getItem('workspaces')));
                        this.trigger('sync', this, options);
                    }.bind(this), 0);
                } else {
                    return Backbone.Collection.prototype.fetch.apply(this, arguments);
                }
            }
        });

        return Workspace;

    });
