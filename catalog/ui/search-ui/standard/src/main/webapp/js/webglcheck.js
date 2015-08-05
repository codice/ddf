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
/*global window*/
/*jslint browser: true*/

define([
        'underscore'
    ],
    function (_) {
        'use strict';

        return {
            isWebglAvailable: undefined,
            isAvailable: function () {
                if (_.isUndefined(this.isWebglAvailable)) {
                    this.isWebglAvailable = false;

                    var context = window.WebGLRenderingContext;

                    var canvas = document.createElement('canvas');

                    var webglOptions = {};

                    // Override select WebGL defaults
                    webglOptions.alpha = false; // WebGL default is true
                    webglOptions.failIfMajorPerformanceCaveat = true; // WebGL default is false

                    try {
                        var gl = canvas.getContext('webgl', webglOptions) || canvas.getContext('experimental-webgl', webglOptions) || undefined;
                        if (gl && context) {
                            this.isWebglAvailable = true;
                        }
                    } catch(e) {}
                }
                return this.isWebglAvailable;
            }
        };
});