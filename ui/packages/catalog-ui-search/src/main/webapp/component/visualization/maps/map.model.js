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
var Backbone = require('backbone');
var MetacardModel = require('js/model/Metacard');
var ol = require('openlayers');

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
        clickDms: undefined,
        target: undefined,
        targetMetacard: undefined
    },
    updateClickCoordinates: function () {
        const lat = this.get('mouseLat');
        const lon = this.get('mouseLon');
        const dms = ol.coordinate.toStringHDMS([lon, lat]);

        this.set({
            clickLat: lat,
            clickLon: lon,
            clickDms: dms
        });
    }
});