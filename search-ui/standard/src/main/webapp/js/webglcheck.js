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

                    //Older firefox needs the experimental check, thin clients may error out while
                    //requesting this though, so best to just wrap with a try
                    //we don't really care, we just want it to fail and not display the map without
                    //breaking the rest of the ui
                    try {
                        var gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
                        if (gl && context) {
                            this.isWebglAvailable = true;
                        }
                    } catch(e) {}
                }
                return this.isWebglAvailable;
            }
        };
});