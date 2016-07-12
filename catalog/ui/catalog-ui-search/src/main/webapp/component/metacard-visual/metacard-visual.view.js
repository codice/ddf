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
    'underscore',
    'jquery',
    'text!./metacard-visual.hbs',
    'js/CustomElements',
    'component/router/router',
    'maptype',
    'component/metacard/metacard',
    'component/visualization/cesium/cesium.view',
    'component/visualization/openlayers/openlayers.view'
], function (wreqr, Marionette, _, $, template, CustomElements, router, maptype,
             metacardInstance, CesiumView, OpenlayersView) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('metacard-visual'),
        modelEvents: {
        },
        events: {
        },
        ui: {
        },
        regions: {
            metacardVisual: '.metacard-visual'
        },
        initialize: function(){
            this.listenTo(router, 'change', this.handleRoute);
            this.handleRoute();
            if (maptype.is3d()) {
                this._mapView = new CesiumView({
                    selectionInterface: metacardInstance
                });
            } else if (maptype.is2d()) {
                this._mapView = new OpenlayersView({
                    selectionInterface: metacardInstance
                });
            }
        },
        handleRoute: function(){
            if (router.toJSON().name === 'openMetacard'){
                this.$el.removeClass('is-hidden');
            } else {
                this.$el.addClass('is-hidden');
            }
        },
        onRender: function(){
            if (this._mapView){
                this.metacardVisual.show(this._mapView);
            }
        }
    });
});
