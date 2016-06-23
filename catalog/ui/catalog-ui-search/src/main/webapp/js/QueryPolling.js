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
/*global define, setInterval, clearInterval*/
define([
    'jquery',
    'underscore',
    'wreqr',
    'js/cql',
    './jquery.whenAll'
], function ($, _, wreqr, cql) {

    //we should probably regex this or find a better way, but for now this works
    function sanitizeGeometryCql(cqlString){
        return cqlString.split("'POLYGON((").join("POLYGON((").split("))'").join("))")
            .split("'POINT(").join("POINT(").split(")'").join(")");
    }

    function buildTimeRangeCQL(when){
        var now = Date.now();
        var then = Date.now() - when;
        now = (new Date(now)).toISOString();
        then = (new Date(then)).toISOString();
        return {
            type: 'OR',
            filters: [
                {
                    type: 'AND',
                    filters: [
                        {
                            property: '"created"',
                            type: 'BEFORE',
                            value: now
                        },
                        {
                            property: '"created"',
                            type: 'AFTER',
                            value: then
                        }
                    ]
                },
                {
                    type: 'AND',
                    filters: [
                        {
                            property: '"modified"',
                            type: 'BEFORE',
                            value: now
                        },
                        {
                            property: '"modified"',
                            type: 'AFTER',
                            value: then
                        }
                    ]
                }
            ]
        };
    }

    function buildPollingCQL(cql, when) {
        return {
            type: 'AND',
            filters: [cql, buildTimeRangeCQL(when)]
        };
    }

    var pollingQueries = {};

    return {
        handleAddingQuery: function(query){
            this.handleRemovingQuery(query);
            query.listenTo(query, 'change:polling', this.handlePollingUpdate.bind(this));
            var polling = query.get('polling');
            if (polling){
                var queryId = query.id;
                var workspaceId = query.collection.parents[0].id;
                var queryClone = query.clone();
                var intervalId = setInterval(function(){
                    queryClone.set('cql',
                        sanitizeGeometryCql("("+ cql.write(cql.simplify(cql.read(cql.write(
                            buildPollingCQL(cql.simplify(cql.read(query.get('cql'))), polling)
                        )))) +")"));
                    $.whenAll.apply(this, queryClone.startSearch()).always(function(){
                        if (pollingQueries[queryId]) {
                            var metacardIds = queryClone.get('result').get('results').map(function (result) {
                                return result.get('metacard').get('properties').get('id');
                            });
                            var when = (new Date(Date.now())).toISOString();
                            if (metacardIds.length > 0) {
                                wreqr.vent.trigger('alerts:add', {
                                    queryId: queryId,
                                    workspaceId: workspaceId,
                                    when: when,
                                    metacardIds: metacardIds
                                });
                            }
                        }
                    });
                }, polling);
                pollingQueries[queryId] = {
                    intervalId: intervalId,
                    queryClone: queryClone
                };
                queryClone.listenTo(query, 'change', this.handleQueryModelUpdate.bind(this));
            }
        },
        handleRemovingQuery: function(query){
            var pollingDetails = pollingQueries[query.id];
            if (pollingDetails){
                clearInterval(pollingDetails.intervalId);
                pollingDetails.queryClone.cancelCurrentSearches();
                delete pollingQueries[query.id];
            }
        },
        handlePollingUpdate: function(query){
            this.handleRemovingQuery(query);
            this.handleAddingQuery(query);
        },
        handleQueryModelUpdate: function(query){
            var pollingDetails = pollingQueries[query.id];
            var changedAttributes = Object.keys(query.changedAttributes());
            if (changedAttributes[0] !== 'result' && pollingDetails){
                pollingDetails.queryClone.set(query.changedAttributes());
            }
        },
        getPollingQueries: function(){
            return pollingQueries;
        }
    };
});