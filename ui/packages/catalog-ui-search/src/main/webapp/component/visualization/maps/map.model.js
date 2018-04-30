/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global require*/
const Backbone = require('backbone');
const MetacardModel = require('js/model/Metacard');
const mtgeo = require('mt-geo');
const Common = require('js/Common');

module.exports = Backbone.AssociatedModel.extend({
    relations: [{
        type: Backbone.One,
        key: 'targetMetacard',
        relatedModel: MetacardModel,
        isTransient: true
    }],
    defaults: {
        mouseLat: undefined,
        mouseLon: undefined,
        clickLat: undefined,
        clickLon: undefined,
        target: undefined,
        targetMetacard: undefined
    },
    isOffMap: function() {
        return this.get('mouseLat') === undefined;
    },
    clearMouseCoordinates: function() {
        this.set({
            mouseLat: undefined,
            mouseLon: undefined
        });
    },
    updateMouseCoordinates: function(coordinates) {
        this.set({
            mouseLat: Number(coordinates.lat.toFixed(6)), // wrap in Number to chop off trailing zero
            mouseLon: Number(Common.wrapMapCoordinates(coordinates.lon, [-180, 180]).toFixed(6))
        });
    },
    updateClickCoordinates: function () {
        const lat = this.get('mouseLat');
        const lon = this.get('mouseLon');
        const dms =  `${mtgeo.toLat(lat)} ${mtgeo.toLon(lon)}`;

        this.set({
            clickLat: lat,
            clickLon: lon,
            clickDms: dms
        });
    }
});