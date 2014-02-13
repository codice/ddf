/*global define*/

define(function (require) {
    'use strict';
    require('purl');
    var $ = require('jquery'),
        _ = require('underscore'),
        webgl = require('webglcheck');

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