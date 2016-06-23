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
/*global define, setTimeout*/
define([
    'marionette',
    'underscore',
    'jquery',
    'text!./metacard-actions.hbs',
    'js/CustomElements',
    'js/store',
    'component/map-actions/map-actions.view'
], function (Marionette, _, $, template, CustomElements, store, MapActions) {

    return Marionette.LayoutView.extend({
        setDefaultModel: function(){
            this.model = this.selectionInterface.getSelectedResults().first();
        },
        className: 'is-list',
        template: template,
        tagName: CustomElements.register('metacard-actions'),
        modelEvents: {
            'all': 'render'
        },
        regions: {
            mapActions: '.map-actions'
        },
        events: {
        },
        ui: {
        },
        selectionInterface: store,
        initialize: function(options){
            this.selectionInterface = options.selectionInterface || this.selectionInterface;
            if (!options.model){
                this.setDefaultModel();
            }
        },
        onRender: function () {
            this.mapActions.show(new MapActions({ model: this.model }));
        }
    });
});
