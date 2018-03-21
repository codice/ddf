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
var Backbone = require('backbone');
var _ = require('underscore');
var $ = require('jquery');
var Sources = require('component/singletons/sources-instance');
var CQLUtils = require('js/CQLUtils');
var Common = require('js/Common');
var filter = require('js/filter');
require('backboneassociations');

var Metacard = require('js/model/Metacard');
var MetacardActionModel = require('js/model/MetacardAction');

function generateThumbnailUrl(url) {
    var newUrl = url;
    if (url.indexOf("?") >= 0) {
        newUrl += '&';
    } else {
        newUrl += '?';
    }
    newUrl += '_=' + Date.now();
    return newUrl;
}

function humanizeResourceSize(result) {
    if (result.metacard.properties['resource-size']) {
        result.metacard.properties['resource-size'] = Common.getFileSize(result.metacard.properties['resource-size']);
    }
}

module.exports = Backbone.AssociatedModel.extend({
    defaults: function () {
        return {
            isResourceLocal: false
        };
    },
    relations: [{
        type: Backbone.One,
        key: 'metacard',
        relatedModel: Metacard
    },{
        type: Backbone.Many,
        key: 'actions',
        collectionType: Backbone.Collection.extend({
            model: MetacardActionModel,
            comparator: function(c) {
                return c.get('title').toLowerCase();
            }
        })
    }],
    initialize: function () {
        this.refreshData = _.throttle(this.refreshData, 200);
    },
    getPreview: function(){
        return this.get('actions').filter((action) => action.title === 'Text Preview')[0].url;
    },
    hasPreview: function(){
        return this.get('actions').filter((action) => action.title === 'Text Preview').length > 0;
    },
    matchesFilters: function(filters) {
        return filter.matchesFilters(this.get('metacard').toJSON(), filters);
    },
    matchesCql: function(cql) {
        return filter.matchesCql(this.get('metacard').toJSON(), cql);
    },
    isWorkspace: function () {
        return this.get('metacard').get('properties').get('metacard-tags').indexOf('workspace') >= 0;
    },
    isResource: function () {
        return this.get('metacard').get('properties').get('metacard-tags').indexOf('resource') >= 0;
    },
    isRevision: function () {
        return this.get('metacard').get('properties').get('metacard-tags').indexOf('revision') >= 0;
    },
    isDeleted: function () {
        return this.get('metacard').get('properties').get('metacard-tags').indexOf('deleted') >= 0;
    },
    isRemote: function () {
        return this.get('metacard').get('properties').get('source-id') !== Sources.localCatalog;
    },
    hasGeometry: function (attribute) {
        return this.get('metacard').hasGeometry(attribute);
    },
    getPoints: function (attribute) {
        return this.get('metacard').getPoints(attribute);
    },
    getGeometries: function (attribute) {
        return this.get('metacard').getGeometries(attribute);
    },
    hasExportActions() {
        return this.getExportActions().length > 0;
    },
    getOtherActions() {
        const otherActions = this.getExportActions().concat(this.getMapActions());
        return this.get('actions').filter((action) => otherActions.indexOf(action) === -1);
    },
    getExportActions() {
        const otherActions = this.getMapActions();
        return this.get('actions').filter((action) => action.get('title').indexOf('Export') === 0).filter((action) => otherActions.indexOf(action) === -1);
    },
    hasMapActions() {
        return this.getMapActions().length > 0;
    },
    getMapActions() {
        return this.get('actions').filter((action) => action.id.indexOf('catalog.data.metacard.map.') === 0);
    },
    refreshData: function () {
        //let solr flush
        setTimeout(function () {
            var metacard = this.get('metacard');
            var req = {
                count: 1,
                cql: CQLUtils.transformFilterToCQL({
                    type: 'AND',
                    filters: [{
                            type: 'OR',
                            filters: [{
                                type: '=',
                                property: '"id"',
                                value: metacard.get('properties').get('metacard.deleted.id') || metacard.id
                            }, {
                                type: '=',
                                property: '"metacard.deleted.id"',
                                value: metacard.id
                            }]
                        },
                        {
                            type: 'ILIKE',
                            property: '"metacard-tags"',
                            value: '*'
                        }
                    ]
                }),
                id: '0',
                sort: 'modified:desc',
                src: metacard.get('properties').get('source-id'),
                start: 1
            };
            $.ajax({
                type: "POST",
                url: '/search/catalog/internal/cql',
                data: JSON.stringify(req),
                contentType: 'application/json'
            }).then(this.parseRefresh.bind(this), this.handleRefreshError.bind(this));

        }.bind(this), 1000);
    },
    handleRefreshError: function () {
        //do nothing for now, should we announce this?
    },
    parseRefresh: function (response) {
        var queryId = this.get('metacard').get('queryId');
        var color = this.get('metacard').get('color');
        _.forEach(response.results, function (result) {
            delete result.relevance;
            result.propertyTypes = response.types[result.metacard.properties['metacard-type']];
            result.metacardType = result.metacard.properties['metacard-type'];
            result.metacard.id = result.metacard.properties.id;
            result.id = result.metacard.id + result.metacard.properties['source-id'];
            result.metacard.queryId = queryId;
            result.metacard.color = color;
            humanizeResourceSize(result);
            var thumbnailAction = _.findWhere(result.actions, {
                id: 'catalog.data.metacard.thumbnail'
            });
            if (result.hasThumbnail && thumbnailAction) {
                result.metacard.properties.thumbnail = generateThumbnailUrl(thumbnailAction.url);
            } else {
                result.metacard.properties.thumbnail = undefined;
            }
        });
        this.set(response.results[0]);
        this.trigger('refreshdata');
    }
});