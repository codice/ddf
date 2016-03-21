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
    'component/content/content'
], function (Backbone, poller, _, Workspace, Source, Workspaces, Selected, Content) {

    // initialize a backbone model and fetch it's state from the server
    var init = function (Model, opts) {
        opts = _.extend({
            persisted: true,
            poll: false
        }, opts);
        var m = new Model();
        if (opts.persisted) {
            m.fetch();
            if (opts.poll) {
                poller.get(m, opts.poll).start();
            }
        }
        return m;
    };

    var Store = Backbone.Model.extend({
        initialize: function () {
            this.set('workspaces', init(Workspace.WorkspaceResult));
            this.set('sources', init(Source, {
                poll: {
                    delay: 60000
                }
            }));
            this.set('componentWorkspaces', init(Workspaces, {
                persisted: false
            }));
            this.set('selected', init(Selected, {
                persisted: false
            }));
            this.set('content', init(Content, {
                persisted: false
            }));
            this.listenTo(this.get('workspaces'), 'change:currentWorkspace', this.clearResults);
        },
        getCurrentWorkspace: function () {
            return this.get('workspaces').get('workspaces').get(this.get('workspaces').get('currentWorkspace'));
        },
        getCurrentQueries: function () {
            return this.getCurrentWorkspace().get('searches');
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
            this.getCurrentQueries().get(query._cloneOf).set(query.attributes);
        },
        clearResults: function(){
            this.get('content').get('results').reset();
        }
    });

    return new Store();
});
