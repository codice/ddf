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

var user = require('component/singletons/user-instance');

define([
    'underscore',
    'backbone',
    'poller',
    'properties',
    'jquery'
], function (_, Backbone, poller, properties, $) {
    "use strict";

    function removeLocalCatalogIfNeeded(response, localCatalog) {
        if (properties.isDisableLocalCatalog()) {
            response = _.filter(response, function(source) {
                return source.id !== localCatalog;
            });
        }

        return response;
    }

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
        comparator: function(a, b) {
            const aName = a.id.toLowerCase();
            const bName = b.id.toLowerCase();
            const aAvailable = a.get('available');
            const bAvailable = b.get('available');
            if ((aAvailable && bAvailable) || (!aAvailable && !bAvailable)){
                if (aName < bName){
                    return -1;
                }
                if (aName > bName) {
                    return 1;
                }
                return 0;
            } else if (!aAvailable){
                return -1;
            } else if (!bAvailable){
                return 1;
            }
        },
        initialize: function () {
            this.listenTo(this, 'change', this.sort);
          this._types = new Types();
          this.determineLocalCatalog();
          this.listenTo(this, 'sync', this.updateLocalCatalog);
        },
        types: function () {
          return this._types;
        },
        parse: function(response) {
            response = removeLocalCatalogIfNeeded(response, this.localCatalog);
            this._types.set(computeTypes(response));
            return response;
        },
        determineLocalCatalog: function(){
            $.get('/search/catalog/internal/localcatalogid').then(function(data){
                this.localCatalog = data['local-catalog-id'];

                poller.get(this, {
                    delay: properties.sourcePollInterval,
                    delayed: properties.sourcePollInterval,
                    continueOnError: true
                }).start();

                this.fetch();
            }.bind(this));
        },
        updateLocalCatalog: function() {
            if (this.get(this.localCatalog)) {
                this.get(this.localCatalog).set('local', true);
            }
        },
        localCatalog: 'local'
    });

});
