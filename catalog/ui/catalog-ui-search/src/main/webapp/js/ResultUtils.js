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
/*global require*/
var store = require('js/store');
var alert = require('component/alert/alert');

module.exports = {
    refreshResult: function(result) {
        var id = result.get('metacard').id;
        result.refreshData();
        store.get('workspaces').forEach(function(workspace) {
            workspace.get('queries').forEach(function(query) {
                if (query.get('result')) {
                    query.get('result').get('results').forEach(function(result) {
                        if (result.get('metacard').get('properties').get('id') === id) {
                            result.refreshData();
                        }
                    });
                }
            });
        });
        alert.get('currentResult').get('results').forEach(function(result) {
            if (result.get('metacard').get('properties').get('id') === id) {
                result.refreshData();
            }
        });
    }
};