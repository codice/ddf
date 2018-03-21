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
var $ = require('jquery');
var _ = require('underscore');
var Backbone = require('backbone');
var Query = require('js/model/Query');
var cql = require('js/cql');
var user = require('component/singletons/user-instance');
var moment = require('moment');
require('backboneassociations');
var WorkspaceModel = require('js/model/Workspace');

module.exports = Backbone.Collection.extend({
    model: WorkspaceModel,
    url: '/search/catalog/internal/workspaces',
    useAjaxSync: true,
    fetched: false,
    handleSync: function () {
        this.fetched = true;
    },
    initialize: function () {
        this.listenTo(this, 'sync', this.handleSync);
        this.handleUserChange();
        this.listenTo(user, 'change', this.handleUserChange);
        var collection = this;
        collection.on('add', function (workspace) {
            workspace.on('change:lastModifiedDate', function () {
                collection.sort();
            });
        });
        this.listenTo(this, 'add', this.tagGuestWorkspace);
    },
    handleUserChange: function () {
        this.fetch({
            remove: false
        });
    },
    tagGuestWorkspace: function (model) {
        if (this.isGuestUser() && model.isNew()) {
            model.set({
                localStorage: true
            });
        }
    },
    isGuestUser: function () {
        return user.get('user').isGuestUser();
    },
    comparator: function (workspace) {
        return -(moment(workspace.get('lastModifiedDate'))).unix();
    },
    createWorkspace: function (title) {
        this.create({
            title: title || 'New Workspace'
        });
    },
    createWorkspaceWithQuery: function (queryModel) {
        this.create({
            title: 'New Workspace',
            queries: [
                queryModel
            ]
        }).get('queries').first().startSearch();
    },
    createAdhocWorkspace: function (text) {
        var cqlQuery;
        var title = text;
        if (text.length === 0) {
            cqlQuery = "anyText ILIKE '%'";
            title = '*';
        } else {
            cqlQuery = "anyText ILIKE '" + cql.translateUserqlToCql(text) + "'";
        }
        var queryForWorkspace = new Query.Model({
            title: title,
            cql: cqlQuery,
            type: 'text'
        });
        this.create({
            title: title,
            queries: [
                queryForWorkspace.toJSON()
            ]
        }).get('queries').first().startSearch();
    },
    createLocalWorkspace: function () {
        var queryForWorkspace = new Query.Model({
            title: 'Example Local',
            federation: 'local',
            excludeUnnecessaryAttributes: false,
            cql: "anyText ILIKE '%'",
            type: 'basic'
        });
        this.create({
            title: 'Template Local',
            queries: [
                queryForWorkspace.toJSON()
            ]
        }).get('queries').first().startSearch();
    },
    createAllWorkspace: function () {
        var queryForWorkspace = new Query.Model({
            title: 'Example Federated',
            federation: 'enterprise',
            excludeUnnecessaryAttributes: false,
            cql: "anyText ILIKE '%'",
            type: 'basic'
        });
        this.create({
            title: 'Template Federated',
            queries: [
                queryForWorkspace.toJSON()
            ]
        }).get('queries').first().startSearch();
    },
    createGeoWorkspace: function () {
        var queryForWorkspace = new Query.Model({
            title: 'Example Location',
            excludeUnnecessaryAttributes: false,
            cql: "anyText ILIKE '%' AND INTERSECTS(anyGeo, POLYGON((-130.7514 20.6825, -130.7514 44.5780, -65.1230 44.5780, -65.1230 20.6825, -130.7514 20.6825)))",
            type: 'basic'
        });
        this.create({
            title: 'Template Location',
            queries: [
                queryForWorkspace.toJSON()
            ]
        }).get('queries').first().startSearch();
    },
    createLatestWorkspace: function () {
        var queryForWorkspace = new Query.Model({
            title: 'Example Temporal',
            excludeUnnecessaryAttributes: false,
            cql: 'anyText ILIKE \'%\' AND ("created" AFTER ' + moment().subtract(1, 'days').toISOString() + ')',
            type: 'basic'
        });
        this.create({
            title: 'Template Temporal',
            queries: [
                queryForWorkspace.toJSON()
            ]
        }).get('queries').first().startSearch();
    },
    duplicateWorkspace: function (workspace) {
        let duplicateWorkspace = _.pick(workspace.toJSON(), 'title', 'queries');
        duplicateWorkspace.queries = duplicateWorkspace.queries.map((query) => _.omit(query, 'isLocal', 'id'));
        this.create(duplicateWorkspace);
    },
    saveAll: function () {
        this.forEach(function (workspace) {
            if (!workspace.isSaved()) {
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
    convert2_10Format: function (localWorkspaceJSON) {
        if (localWorkspaceJSON.constructor === Array) {
            return localWorkspaceJSON.reduce(function (blob, workspace) {
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