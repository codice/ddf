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
/*global define*/
define([
    'wreqr',
    'marionette',
    'js/CustomElements',
    './visualization.hbs',
    'component/visualization/cesium/cesium.view',
    'component/visualization/openlayers/openlayers.view',
    'component/visualization/histogram/histogram.view',
    'component/singletons/user-instance',
    'maptype'
], function (wreqr, Marionette, CustomElements, template, CesiumView, OpenlayersView, HistogramView,
             user, maptype) {

    function getActiveVisualization() {
        return user.get('user').get('preferences').get('visualization');
    }

    function getPreferences() {
        return user.get('user').get('preferences');
    }

    return Marionette.LayoutView.extend({
        tagName: CustomElements.register('visualization'),
        template: template,
        regions: {
            activeVisualization: '.visualization-container'
        },
        events: {
        },
        initialize: function(){
            this.listenTo(getPreferences(), 'change:visualization', this.onBeforeShow);
        },
        onBeforeShow: function(){
            switch(getActiveVisualization()){
                case 'map':
                    this.showMap();
                    break;
                case 'histogram':
                    this.showHistogram();
                    break;
            }
        },
        showOpenlayers: function(){
            this.activeVisualization.show(new OpenlayersView({
                selectionInterface: this.options.selectionInterface
            }));
        },
        showCesium: function(){
            this.activeVisualization.show(new CesiumView({
                selectionInterface: this.options.selectionInterface
            }));
        },
        showMap: function(){
            if (maptype.is3d()) {
                this.showCesium();
            } else if (maptype.is2d()) {
                this.showOpenlayers();
            }
        },
        showHistogram: function(){
            this.activeVisualization.show(new HistogramView({
                selectionInterface: this.options.selectionInterface
            }));
        }
    });

});