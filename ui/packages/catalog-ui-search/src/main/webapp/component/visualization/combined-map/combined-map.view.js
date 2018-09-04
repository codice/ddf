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
/*global require, window*/
var _ = require('underscore');
var $ = require('jquery');
var wreqr = require('wreqr');
var template = require('./combined-map.hbs');
var Marionette = require('marionette');
var CustomElements = require('js/CustomElements');
var CesiumView = require('component/visualization/maps/cesium/cesium.view');
var OpenlayersView = require('component/visualization/maps/openlayers/openlayers.view');
var Common = require('js/Common');
var store = require('js/store');
var user = require('component/singletons/user-instance');
var featureDetection = require('component/singletons/feature-detection');

module.exports = Marionette.LayoutView.extend({
    tagName: CustomElements.register('combined-map'),
    template: template,
    regions: {
        mapContainer: '> .map-container'
    },
    onRender: function(){
        this.listenToOnce(featureDetection, 'change:cesium', this.render);
        if (featureDetection.supportsFeature('cesium')){
            this.mapContainer.show(new CesiumView(this.options));
        } else {
            this.mapContainer.show(new OpenlayersView(this.options));
        }
    }
});