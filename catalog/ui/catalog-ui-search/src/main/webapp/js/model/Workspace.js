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
/*global define, window*/

define([
        'backbone',
        'js/model/Metacard',
        'js/model/Query',
        'js/Common',
        'backboneassociations'
    ],
    function (Backbone, Metacard, Query, Common) {
        "use strict";
        var Workspace = {};

        Workspace.MetacardList = Backbone.Collection.extend({
            model: Metacard.Metacard
        });

        Workspace.SearchList = Backbone.Collection.extend({
            model: Query.Model,
            canAddQuery: function(){
                return this.length < 10;
            }
        });

        var fakeUsers = ['Andrew', 'Chris', 'Korben', 'Leon', 'Krauser','Wesker'];
        var fakeShares = ['Andrew', 'Chris', 'Team 1', 'Team 2'];
        var fakeSources = ['local', 'Remote 1', 'Remote 2'];
        var getRandomValue = function(arr){
            return arr[Math.floor(arr.length*Math.random())];
        };
        var getMultipleRandomValue = function(arr){
            var randomValues = [];
            var multiple = Math.ceil(Math.random()*2);
            for (var i = 0; i<multiple; i++){
                randomValues.push(getRandomValue(arr));
            }
            return randomValues;
        };

        Workspace.Model = Backbone.AssociatedModel.extend({
            relations: [
                {
                    type: Backbone.Many,
                    key: 'searches',
                    collectionType: Workspace.SearchList
                },
                {
                    type: Backbone.Many,
                    key: 'metacards',
                    relatedModel: Metacard.MetacardResult
                }
            ],
            canAddQuery: function(){
              return this.get('searches').length < 10;
            },
            addQuery: function () {
                var query = new Query.Model({title: 'New Workspace'});
                this.get('searches').add(query);
                return query.get('id');
            },
            getSelectedQuery: function () {
                return this.get('searches').find(function (query) {
                    return query.get('selected');
                });
            },
            initialize: function() {
                if(!this.get('searches')) {
                    this.set({searches: new Workspace.SearchList()});
                }
                if(!this.get('metacards')) {
                    this.set({metacards: new Workspace.MetacardList()});
                }
                this.addMetacardProperties();
                this.get('searches').on('add',function(){
                    this.trigger('change');
                });
                this.on('nested-change',function(){
                    var attributesChanged = Object.keys(this.changedAttributes());
                    if (!(attributesChanged.length === 1 && attributesChanged[0] === 'lastModifiedDate' && attributesChanged[0] === 'history')){
                        this.updateLastModifiedDate();
                    }
                });
                this.on('change',function(){
                    var attributesChanged = Object.keys(this.changedAttributes());
                    if (!(attributesChanged.length === 1 && attributesChanged[0] === 'lastModifiedDate' && attributesChanged[0] === 'history')){
                        this.updateLastModifiedDate();
                    }
                });
            },
            updateLastModifiedDate: function(){
                this.set('lastModifiedDate',(new Date()).toLocaleString());
                this.addNewHistory();
            },
            addNewHistory: function(){
                var history = this.get('history') || [];
                history.unshift({
                    version: history.length + 1,
                    date: this.get('lastModifiedDate'),
                    user: getRandomValue(fakeUsers),
                    id: Common.generateUUID()
                });
                this.set('history',history);
            },
            addMetacardProperties: function(){
                if (this.get('id')===undefined){
                    this.set('id', Common.generateUUID());
                }
                if (this.get('createdDate')===undefined){
                    this.set('createdDate', (new Date()).toLocaleString());
                }
                if (this.get('lastModifiedDate')===undefined){
                    this.updateLastModifiedDate();
                }
                if (this.get('sharedWith') === undefined){
                    this.set('sharedWith',getMultipleRandomValue(fakeShares));
                }
                if (this.get('source')=== undefined){
                    this.set('source',getRandomValue(fakeSources));
                }
                if (this.get('local')=== undefined){
                    this.set('local',this.get('source')==='local');
                }
            }
        });

        Workspace.WorkspaceList = Backbone.Collection.extend({
            model: Workspace.Model,
            initialize: function(){
                var collection = this;
                collection.on('add',function(workspace){
                    workspace.on('change:lastModifiedDate',function(){
                        collection.sort();
                    });
                });
            },
            comparator: function(workspace){
                return -(new Date(workspace.get('lastModifiedDate'))).getTime();
            }
        });

        Workspace.WorkspaceResult = Backbone.AssociatedModel.extend({
            relations: [
                {
                    type: Backbone.Many,
                    key: 'workspaces',
                    relatedModel: Workspace.Model,
                    collectionType: Workspace.WorkspaceList
                }
            ],
            defaults: {
                currentWorkspace: undefined
            },
            url: '/service/workspaces',
            useAjaxSync: false,
            initialize: function() {
                if(!this.get('workspaces')) {
                    this.set({workspaces: new Workspace.WorkspaceList()});
                }
                this.on({
                   'all': this.setDefaultCurrentWorkspace
                });
                /*this.on('nested-change', function () {
                    this.save();
                });
                this.on('change', function () {
                    this.save();
                });*/
            },
            parse: function (resp) {
                if (resp.data) {
                    return resp.data;
                }
                return resp;
            },
            clearDeletedWorkspace: function(){
                var currentWorkspace = this.get('currentWorkspace');
                if (currentWorkspace && !this.get('workspaces').get(currentWorkspace)){
                    this.set('currentWorkspace',undefined);
                }
            },
            setDefaultCurrentWorkspace: function(){
                this.clearDeletedWorkspace();
                var currentWorkspace = this.get('currentWorkspace');
                var workspaces = this.get('workspaces');
                if (!currentWorkspace && workspaces.length !==0){
                    this.set('currentWorkspace',workspaces.models[0].get('id'));
                }
            },
            getCurrentWorkspace: function () {
                return this.getWorkspace(this.get('currentWorkspace'));
            },
            getCurrentWorkspaceName: function(){
                var currentWorkspace = this.getCurrentWorkspace();
                if (currentWorkspace){
                    return currentWorkspace.get('name');
                }
            },
            createWorkspace: function(){
                var workspace = new Workspace.Model({name: 'New Workspace'});
                this.get('workspaces').add(workspace);
                this.save();
                return workspace.get('id');
            },
            setCurrentWorkspace: function(workspaceId){
                console.log(workspaceId);
                this.set('currentWorkspace', workspaceId);
            },
            getWorkspace: function(workspaceId){
                return this.get('workspaces').get(workspaceId);
            }
        });

        window.Workspace = Workspace;

        return Workspace;

    });
