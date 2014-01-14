/*global define*/
/*jslint browser: true*/

define(function (require) {
    'use strict';
    var _ = require('underscore');

    return {
        isWebglAvailable: undefined,
        isAvailable: function () {
            if (_.isUndefined(this.isWebglAvailable)) {
                this.isWebglAvailable = false;

                if (window.WebGLRenderingContext) {
                    this.isWebglAvailable = true;
                }

                var canvas = document.createElement('canvas');
                var gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
                if (gl) {
                    this.isWebglAvailable = true;
                }
            }
            return this.isWebglAvailable;
        }
    };
});