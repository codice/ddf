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
    'underscore',
    'backbone',
    'component/input/input',
    'component/input/thumbnail/input-thumbnail',
    'component/input/date/input-date',
    'component/input/bulk/input-bulk'
], function (_, Backbone, TextInput, ThumbnailInput, DateInput, BulkInput) {

    var MetacardInputCollection = Backbone.Collection.extend({
        model: function(attrs, options){
            if (options.multivalued){
                return new BulkInput(attrs);
            } else {
                switch (options.type) {
                    case 'DATE':
                        return new DateInput(attrs);
                        break;
                    case 'STRING':
                        return new TextInput(attrs);
                        break;
                    case 'GEOMETRY':
                        return new TextInput(attrs);
                        break;
                    case 'XML':
                        return new TextInput(attrs);
                        break;
                    case 'BINARY':
                        return new ThumbnailInput(attrs);
                        break;
                    default:
                        return new TextInput(attrs);
                        break;
                }
            }
        },
        initialize: function(options){

        }
    }, {
        hiddenTypes: ['XML', 'BINARY', 'OBJECT'],
        blacklist: ['metacard-type', 'source-id'],
        create: function(searchResult){
            var self = this;
            var metacardInputCollection = new MetacardInputCollection();
            metacardInputCollection.comparator = function(model){
                return model.id;
            };
            var propertyTypes = searchResult.get('propertyTypes');
            searchResult.get('metacard').get('properties').pairs().forEach(function(pair){
                if (pair[0].indexOf('metadata')!==0 && self.blacklist.indexOf(pair[0]) === -1
                    && self.hiddenTypes.indexOf(propertyTypes[pair[0]]['format']) === -1) {
                    metacardInputCollection.add({
                        id: pair[0],
                        value: pair[1]
                    }, {
                        type: propertyTypes[pair[0]]['format']
                    });
                }
            });
            return metacardInputCollection;
        },
        createBasic: function(searchResult){
            var metacardInputCollection = new MetacardInputCollection();
            var propertyTypes = searchResult.get('propertyTypes');
            var metacard = searchResult.get('metacard');
            metacardInputCollection.add({
                id: 'source-id',
                value: metacard.get('properties').get('source-id'),
                label: 'Source'
            }, {
                type: 'STRING'
            });
            metacardInputCollection.add({
                id: 'cached',
                value: metacard.get('cached'),
                label: 'Current as of'
            }, {
                type: 'DATE'
            });
            metacardInputCollection.add({
                id: 'created',
                value: metacard.get('properties').get('created'),
                label: 'Created'
            }, {
            type: 'DATE'
            });
            metacardInputCollection.add({
                id: 'modified',
                value: metacard.get('properties').get('modified'),
                label: 'Modified'
            }, {
                type: 'DATE'
            });
            metacardInputCollection.add({
                id: 'thumbnail',
                value: metacard.get('properties').get('thumbnail'),
                label: 'Thumbnail'
            }, {
                type: 'BINARY'
            });
            return metacardInputCollection;
        },
        createWorkspaceBasic: function(workspace){
            var metacardInputCollection = new MetacardInputCollection();
            metacardInputCollection.add({
                id: 'title',
                value: workspace.get('title'),
                label: 'Title'
            }, {
                type: 'STRING'
            });
            metacardInputCollection.add({
                id: 'created',
                value: workspace.get('created'),
                label: 'Created'
            }, {
                type: 'DATE'
            });
            metacardInputCollection.add({
                id: 'modified',
                value: workspace.get('modified'),
                label: 'Modified'
            }, {
                type: 'DATE'
            });
            return metacardInputCollection;
        },
        createWorkspaceAdvanced: function(workspace){
            var metacardInputCollection = new MetacardInputCollection();
            metacardInputCollection.add({
                id: 'title',
                value: workspace.get('title'),
                label: 'Title'
            }, {
                type: 'STRING'
            });
            metacardInputCollection.add({
                id: 'created',
                value: workspace.get('created'),
                label: 'Created'
            }, {
                type: 'DATE'
            });
            metacardInputCollection.add({
                id: 'modified',
                value: workspace.get('modified'),
                label: 'Modified'
            }, {
                type: 'DATE'
            });
            metacardInputCollection.add({
                id: 'id',
                value: workspace.get('id'),
                label: 'ID'
            }, {
                type: 'STRING'
            });
            return metacardInputCollection;
        },
        createBulkBasic: function(metacards){
            var self = this;
            var metacardInputCollection = new MetacardInputCollection();
            var metacardsJSON = metacards.toJSON();
            var propertyIntersection = _.intersection.apply(_, metacardsJSON.map(function(metacard){
                return Object.keys(metacard.metacard.properties);
            })).filter(function(property){
                return property.indexOf('metadata')!==0 && self.blacklist.indexOf(property) === -1
                    && self.hiddenTypes.indexOf(metacardsJSON[0].propertyTypes[property].format) === -1;
            });
            var propertyArray = [];
            propertyIntersection.forEach(function(property){
                propertyArray.push({
                    id: property,
                    type: metacardsJSON[0].propertyTypes[property].format,
                    values: {}
                });
            });
            propertyArray.forEach(function(property){
                metacardsJSON.forEach(function(metacard){
                    var value = metacard.metacard.properties[property.id];
                    property.values[value] = property.values[value] || {
                            value: value,
                            hits: 0
                        };
                    property.values[value].hits++;
                });
            });
            propertyArray.forEach(function(property){
                var hasSingleValue = Object.keys(property.values).length === 1;
                if (hasSingleValue){
                    metacardInputCollection.add({
                        id: property.id,
                        value: Object.keys(property.values)[0]
                    }, {
                        type: property.type
                    });
                } else {
                    metacardInputCollection.add({
                        id: property.id,
                        values: property.values,
                        type: property.type
                    }, {
                        multivalued: true,
                        type: property.type
                    });
                }
            });
            return metacardInputCollection;
        }
    });

    return MetacardInputCollection;
});