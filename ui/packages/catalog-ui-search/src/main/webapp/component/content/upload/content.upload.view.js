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
    'js/CustomElements',
    '../content.view',
    'js/store',
    'js/Common',
    'component/upload/upload',
    'component/result-selector/result-selector.view'
], function (wreqr, Marionette, _, $, CustomElements, ContentView, store, Common,
             uploadInstance, ResultSelectorView) {

    return ContentView.extend({
        className: 'is-upload',
        setupListener: function () {
            this.listenTo(uploadInstance, 'change:currentUpload', this.updatePanelOne);
        },
        updatePanelOne: function(){
            this.panelOne.show(new ResultSelectorView({
                model: uploadInstance.get('currentQuery'),
                selectionInterface: uploadInstance
            }));
        },
        _mapView: undefined
    });
});