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
/*global require, setTimeout*/
//You typically don't want to use this view directly.  Instead, use the combined-map component which will handle falling back to openlayers.

var MapView = require('../map.view');
var template = require('./cesium.hbs');
var $ = require('jquery');
var user = require('component/singletons/user-instance');
var _ = require('underscore');
var featureDetection = require('component/singletons/feature-detection');

module.exports = MapView.extend({
    template: template,
    className: 'is-cesium',
    events: function() {
        return _.extend({
            'click > .not-supported button': 'switchTo2DMap'
        }, MapView.prototype.events);
    },
    loadMap: function() {
        var deferred = new $.Deferred();
        require([
            './map.cesium'
        ], function(CesiumMap) {
            deferred.resolve(CesiumMap);
        });
        return deferred;
    },
    createMap: function(){
        try {
            MapView.prototype.createMap.apply(this, arguments);
        } catch (err){
            this.$el.addClass('not-supported');
            setTimeout(function(){
                this.switchTo2DMap();
            }.bind(this), 10000);
            this.endLoading();
        }
    },
    switchTo2DMap: function(){
        if (!this.isDestroyed){
            featureDetection.addFailure('cesium');
        }
    }
});