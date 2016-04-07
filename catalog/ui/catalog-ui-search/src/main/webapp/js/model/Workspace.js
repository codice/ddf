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
        'wreqr',
        'backbone',
        'js/model/Metacard',
        'js/model/Query',
        'js/Common',
        'js/ColorGenerator',
        'backboneassociations'
    ],
    function (wreqr, Backbone, Metacard, Query, Common, ColorGenerator) {
        var Workspace = {};

        Workspace.MetacardCollection = Backbone.Collection.extend({
            model: Metacard.Metacard
        });

        Workspace.QueryCollection = Backbone.Collection.extend({
            model: Query.Model,
            initialize: function(){
                var searchList = this;
                this._colorGenerator = ColorGenerator.getNewGenerator();
                this.listenTo(this, 'add', function(query){
                    query.setColor(searchList._colorGenerator.getColor(query.getId()));
                    query.startSearch();
                    query.listenTo(query, 'change', function(){
                        query.startSearch();
                    });
                });
                this.listenTo(this, 'remove', function(query){
                    searchList._colorGenerator.removeColor(query.getId);
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
                },
                {
                    type: Backbone.Many,
                    key: 'metacards',
                    collectionType: Workspace.MetacardCollection
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
            }
        });

        Workspace.Collection = Backbone.Collection.extend({
            model: Workspace.Model,
            url: '/services/search/catalog/workspaces',
            useAjaxSync: true,
            initialize: function(){
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
            comparator: function(workspace){
                return -(new Date(workspace.get('lastModifiedDate'))).getTime();
            },
            createWorkspace: function(title){
               this.create({title: title || 'New Workspace'});
             }
        });

        return Workspace;

    });
