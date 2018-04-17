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
/*global define, window*/
define([
    'wreqr',
    'marionette',
    'underscore',
    'jquery',
    './content.hbs',
    'js/CustomElements',
    'component/toolbar/toolbar.view',
    'js/store',
    'js/Common',
    'component/golden-layout/golden-layout.view'
], function (wreqr, Marionette, _, $, contentTemplate, CustomElements, ToolbarView,
              store, Common, GoldenLayoutView) {

    var ContentView = Marionette.LayoutView.extend({
        template: contentTemplate,
        tagName: CustomElements.register('content'),
        regions: {
            'toolbar': '.content-toolbar',
            'panelOne': '.content-panelOne',
            'panelThree': '.content-panelThree'
        },
        initialize: function(){
            this._mapView = new GoldenLayoutView({
                selectionInterface: store.get('content'),
                configName: 'goldenLayout'
            });
            this.setupListener();
        },
        setupListener: function () {
            //override in extended views
        },
        onRender: function(){
            this.updatePanelOne();
            if (this._mapView){
                this.panelThree.show(this._mapView);
                this.toolbar.show(new ToolbarView({goldenLayout: this._mapView.goldenLayout}));
            }
        },
        updatePanelOne: function(workspace){
            //override in extended views
        },
        _mapView: undefined

    });

    return ContentView;
});
