/*global define*/
/*jslint browser: true*/

define(function (require) {
    'use strict';
    var _ = require('underscore');

    return {
        isWebglAvailable: undefined,
        isAvailable: function () {
            if (_.isUndefined(this.isWebglAvailable)) {
                this.isWebglAvailable = true;

                if (!window.WebGLRenderingContext) {
                    this.isWebglAvailable = false;
                }

                var canvas = document.createElement('canvas');
                var gl = canvas.getContext('webgl');
                if (!gl) {
                    this.isWebglAvailable = false;
                }
            }
            return this.isWebglAvailable;
        }
    };
});