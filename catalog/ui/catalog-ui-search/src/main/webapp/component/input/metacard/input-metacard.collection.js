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
    'component/input/thumbnail/input-thumbnail'
], function (_, Backbone, TextInput, ThumbnailInput) {

    var MetacardInputCollection = Backbone.Collection.extend({
        model: function(attrs, options){
            switch (options.type) {
                case 'DATE':
                    return new TextInput(attrs);
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
        }
    });

    return MetacardInputCollection;
});