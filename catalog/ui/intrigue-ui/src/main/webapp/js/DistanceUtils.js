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
/*jshint bitwise: false*/
define([
], function () {

    var EARTH_MEAN_RADIUS_METERS = 6371008.7714;

    var DEGREES_TO_RADIANS =  Math.PI / 180;
    var RADIANS_TO_DEGREES =  1 / DEGREES_TO_RADIANS;


    return {
        distToDegrees: function(distanceInMeters){
            return this.toDegrees(this.distToRadians(distanceInMeters));
        },
        distToRadians: function(distanceInMeters){
            return distanceInMeters / EARTH_MEAN_RADIUS_METERS;
        },
        toDegrees: function(distanceInRadians){
            return distanceInRadians * RADIANS_TO_DEGREES;
        }
    };
});