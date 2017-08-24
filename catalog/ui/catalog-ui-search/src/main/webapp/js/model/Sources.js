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
    'poller',
    'properties',
    'jquery'
], function (_, Backbone, poller, properties, $) {
    "use strict";

    var Types = Backbone.Collection.extend({
    });

    var computeTypes = function (sources) {
        if (_.size(properties.typeNameMapping) > 0) {
            return _.map(properties.typeNameMapping, function(value, key) {
                if (_.isArray(value)) {
                    return {
                        name: key,
                        value: value.join(',')
                    };
                }
            });
        } else {
            return _.chain(sources)
                .map(function (source) {
                    return source.contentTypes;
                })
                .flatten()
                .filter(function (element) {
                    return element.name !== '';
                })
                .sortBy(function (element) {
                    return element.name.toUpperCase();
                })
                .uniq(false, function (type) {
                    return type.name;
                })
                .map(function (element) {
                    element.value = element.name;
                    return element;
                })
                .value();
        }
    };

    return Backbone.Collection.extend({
        url: "/services/catalog/sources",
        useAjaxSync: true,
        initialize: function () {
            this.listenTo(this, 'update add', _.debounce(this.determineWritableSources, 60));
            this._types = new Types();
            poller.get(this, {
                delay: properties.sourcePollInterval,
                delayed: properties.sourcePollInterval,
                continueOnError: true
            }).start();
            this.determineLocalCatalog();
            this.fetch({async: false});
        },
        types: function () {
          return this._types;
        },
        parse: function(response) {
            this._types.set(computeTypes(response));
            return response;
        },
        determineWritableSources: function(){
            $.get('/search/catalog/internal/writablesources').then(function(writableSources){
                this.forEach(function(sourceModel){
                    sourceModel.set('writable', writableSources.indexOf(sourceModel.id) >= 0);
                }.bind(this));
            }.bind(this));
        },  
        determineLocalCatalog: function(){
            $.get('/search/catalog/internal/localcatalogid').then(function(data){
                this.localCatalog = data['local-catalog-id'];
            }.bind(this));
        },
        localCatalog: 'local'
    });

});
