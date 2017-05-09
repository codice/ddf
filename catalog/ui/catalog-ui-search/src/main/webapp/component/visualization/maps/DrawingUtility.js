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
/*global require, document*/
//allows us to get around svg security restrictions in IE11 (see using svg in opengl)
//make our own image and manually set dimensions because of IE: https://github.com/openlayers/openlayers/issues/3939
var _ = require('underscore');
var defaultColor = '#3c6dd5';

module.exports = {
    getCircle: function(options) {
        _.defaults(options, {
            diameter: 22,
            fillColor: defaultColor,
            strokeWidth: 2,
            strokeColor: 'white'
        });
        var radius = options.diameter / 2;
        var canvas = document.createElement('canvas');
        canvas.width = options.diameter;
        canvas.height = options.diameter;
        var ctx = canvas.getContext("2d");
        ctx.beginPath();
        ctx.strokeStyle = options.strokeColor;
        ctx.lineWidth = options.strokeWidth;
        ctx.fillStyle = options.fillColor;
        ctx.arc(radius, radius, radius - options.strokeWidth / 2, 0, 2 * Math.PI, false);
        ctx.fill();
        ctx.stroke();
        return canvas;
    },
    getCircleWithText: function(options) {
        _.defaults(options, {
            diameter: 44,
            fillColor: defaultColor,
            strokeWidth: 2,
            strokeColor: 'white',
            text: '',
            textColor: 'white'
        });
        var canvas = this.getCircle(options);
        var ctx = canvas.getContext("2d");
        ctx.font = '16pt Helvetica';
        ctx.fillStyle = options.textColor;
        ctx.textAlign = 'center';
        ctx.textBaseline = "middle";
        ctx.fillText(options.text, options.diameter/2, options.diameter/2);
        return canvas;
    }
};