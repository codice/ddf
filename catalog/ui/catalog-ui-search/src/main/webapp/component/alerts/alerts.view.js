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
    'text!./alerts.hbs',
    'js/CustomElements',
    'component/alert-item/alert-item.collection.view'
], function (Marionette, _, $, template, CustomElements, AlertItemCollectionView) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('alerts'),
        modelEvents: {},
        events: {
        },
        regions: {
            alertList: '.alerts-list'
        },
        ui: {},
        onBeforeShow: function () {
            this.alertList.show(new AlertItemCollectionView());
        }
    });
});
