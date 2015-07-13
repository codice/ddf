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
        'jquery',
        'underscore',
        'webglcheck',
        'twodcheck',
        'purl'
    ],
    function ($, _, webgl, twoD) {
        'use strict';

        var MapTypeEnum = {
            THREED: '3d',
            TWOD: '2d',
            NONE: 'none'
        };

        return {
            type: function () {
                var param = $.url().param('map');
                if (!_.isUndefined(param)) {
                    if (_.contains(_.values(MapTypeEnum), param)) {
                        return param;
                    }
                }

                if (webgl.isAvailable()) {
                    return MapTypeEnum.THREED;
                } else if (twoD.isAvailable()) {
                    return MapTypeEnum.TWOD;
                } else {
                    return MapTypeEnum.NONE;
                }
            }(),

            is3d: function () {
                return this.type === MapTypeEnum.THREED;
            },
            is2d: function () {
                return this.type === MapTypeEnum.TWOD;
            },
            isNone: function () {
                return this.type === MapTypeEnum.NONE;
            },
            isMap: function () {
                return this.is3d() || this.is2d();
            }
        };
});