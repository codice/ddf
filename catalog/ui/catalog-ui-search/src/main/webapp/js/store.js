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
    'jquery',
    'backbone',
    'poller',
    'underscore',
    'js/model/Workspace',
    'js/model/source',
    'js/model/user',
    'js/model/Selected',
    'component/content/content',
    'component/router/router',
    'application',
    'properties'
], function ($, Backbone, poller, _, Workspace, Source, User, Selected, Content, Router, Application, properties) {

    return new (Backbone.Model.extend({
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
            var model = new Model({ store: this });
            this.setupListeners(model, opts.listeners);
            this.setupPolling(model, opts);
            return model;
        },
        initialize: function () {
            this.set('user', Application.UserModel);
            this.set('content', this.initModel(Content, {
                persisted: false
            }));
            this.set('workspaces', this.initModel(Workspace.Collection));
            this.set('sources', Source);
            this.set('selected', this.initModel(Selected, {
                persisted: false
            }));
            this.set('router', this.initModel(Router, {
                persisted: false
            }));
            this.getMetacardTypes();
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
        addMetacardTypes: function(metacardTypes){
            this.get('content').addMetacardTypes(metacardTypes);
        },
        saveCurrentWorkspace: function(){
            this.getCurrentWorkspace().save();
        },
        getEnums: function(){
            $.when.apply(this, this.metacardDefinitions.map(function(metacardDefinition){
                return $.get( '/search/catalog/internal/enumerations/'+metacardDefinition);
            })).always(function(){
                _.forEach(arguments, function(response){
                    _.extend(this.enums, response[0]);
                }.bind(this));
            }.bind(this));
        },
        getMetacardTypes: function(){
            $.get('/search/catalog/internal/metacardtype').then(function(metacardTypes){
                for (var metacardType in metacardTypes){
                    if (metacardTypes.hasOwnProperty(metacardType)) {
                        this.metacardDefinitions.push(metacardType);
                        for (var type in metacardTypes[metacardType]) {
                            if (metacardTypes[metacardType].hasOwnProperty(type)) {
                                this.metacardTypes[type] = metacardTypes[metacardType][type];
                                this.metacardTypes[type].alias = properties.attributeAliases[type];
                            }
                        }
                    }
                }
                for (var propertyType in this.metacardTypes){
                    if (this.metacardTypes.hasOwnProperty(propertyType)) {
                        this.sortedMetacardTypes.push(this.metacardTypes[propertyType]);
                    }
                }
                this.sortedMetacardTypes.sort(function(a, b){
                    if (a.id < b.id){
                        return -1;
                    }
                    if (a.id > b.id){
                        return 1;
                    }
                    return 0;
                });
                this.getEnums();
            }.bind(this));
        },
        metacardDefinitions: [],
        sortedMetacardTypes: [],
        metacardTypes: {
            anyText: {
                id: 'anyText',
                type: 'STRING',
                multivalued: false
            },
            anyGeo: {
                id: 'anyGeo',
                type: 'LOCATION',
                multivalued: false
            }
        },
        enums: {
        }
    }))();
});
