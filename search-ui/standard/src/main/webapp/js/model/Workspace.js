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
        'js/model/Metacard',
        'js/model/Query',
        'backbonerelational'
    ],
    function (Backbone, Metacard, Query) {
        "use strict";
        var Workspace = {};

        Workspace.MetacardList = Backbone.Collection.extend({
            model: Metacard.Metacard
        });

        Workspace.QueryList = Backbone.Collection.extend({
            model: Query.Model
        });

        Workspace.Workspace = Backbone.RelationalModel.extend({
            relations: [
                {
                    type: Backbone.HasMany,
                    key: 'searches',
                    relatedModel: Query.Model,
                    collectionType: Workspace.QueryList,
                    includeInJSON: true,
                    reverseRelation: {
                        key: 'workspace'
                    }
                },
                {
                    type: Backbone.HasMany,
                    key: 'metacards',
                    relatedModel: Metacard.Metacard,
                    collectionType: Workspace.MetacardList,
                    includeInJSON: true,
                    reverseRelation: {
                        key: 'workspace'
                    }
                }
            ]
        });

        Workspace.WorkspaceList = Backbone.Collection.extend({
            model: Workspace.Workspace
        });

        Workspace.WorkspaceResult = Backbone.RelationalModel.extend({
            defaults: {
                workspaces: new Workspace.WorkspaceList()
            },
            relations: [
                {
                    type: Backbone.HasMany,
                    key: 'workspaces',
                    relatedModel: Workspace.Workspace,
                    collectionType: Workspace.WorkspaceList,
                    includeInJSON: true,
                    reverseRelation: {
                        key: 'workspaceResult'
                    }
                }
            ],
            url: '/service/workspaces',
            useAjaxSync: false,
            parse: function (resp) {
                if (resp.data) {
                    return resp.data;
                }
                return resp;
            }
        });

        return Workspace;

    });