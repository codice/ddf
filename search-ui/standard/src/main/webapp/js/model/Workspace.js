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
        'backboneassociations'
    ],
    function (Backbone, Metacard, Query) {
        "use strict";
        var Workspace = {};

        Workspace.SearchList = Backbone.Collection.extend({
            model: Query.Model
        });

        Workspace.Model = Backbone.AssociatedModel.extend({
            relations: [
                {
                    type: Backbone.Many,
                    key: 'searches',
                    relatedModel: Query.Model
                },
                {
                    type: Backbone.Many,
                    key: 'metacards',
                    relatedModel: Metacard.Metacard
                }
            ]
        });

        Workspace.WorkspaceList = Backbone.Collection.extend({
            model: Workspace.Model
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