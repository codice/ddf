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
var metacardDefinitions = require('component/singletons/metacard-definitions');
var Turf = require('@turf/turf');
var TurfMeta = require('@turf/meta');
var wkx = require('wkx');
var properties = require('properties');
require('backbone-associations');

module.exports = Backbone.AssociatedModel.extend({
    defaults: function () {
        return {
            'metacard-tags': ['resource']
        }
    },
    hasGeometry: function (attribute) {
        return _.filter(this.toJSON(), function (value, key) {
            return (attribute === undefined || attribute === key) && metacardDefinitions.metacardTypes[key] &&
                metacardDefinitions.metacardTypes[key].type === "GEOMETRY";
        }).length > 0;
    },
    getCombinedGeoJSON: function () {
        return;
    },
    getPoints: function (attribute) {
        return this.getGeometries(attribute).reduce(function (pointArray, wkt) {
            return pointArray.concat(TurfMeta.coordAll(wkx.Geometry.parse(wkt).toGeoJSON()));
        }, []);
    },
    getGeometries: function (attribute) {
        return _.filter(this.toJSON(), function (value, key) {
            return !properties.isHidden(key) && (attribute === undefined || attribute === key) && metacardDefinitions.metacardTypes[key] &&
                metacardDefinitions.metacardTypes[key].type === "GEOMETRY";
        });
    }
});