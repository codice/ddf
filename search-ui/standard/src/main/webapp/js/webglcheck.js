/*global define*/
/*global window*/
/*jslint browser: true*/

define(function (require) {
    'use strict';
    var _ = require('underscore');

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
                try{
                  var gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
                  if (!_.isUndefined(gl) && !_.isUndefined(context)){
                    this.isWebglAvailable = true;
                  }
                }catch(e){}
            }
            return this.isWebglAvailable;
        }
    };
});