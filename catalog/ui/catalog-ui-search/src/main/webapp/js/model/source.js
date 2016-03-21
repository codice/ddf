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
    'properties'
], function (_, Backbone, properties) {
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
            return sources.chain()
                .map(function (source) {
                    return source.get('contentTypes');
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

    var Sources = Backbone.Collection.extend({
        url: "/services/catalog/sources",
        useAjaxSync: true,
        initialize: function () {
          this._types = new Types();
          this.on('change', function () {
            this._types.set(computeTypes(this));
          }.bind(this));
        },
        types: function () {
          return this._types;
        }
    });

    return Sources;

});
