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
/*global define, alert*/
define([
    'marionette',
    'underscore',
    'jquery',
    'js/CustomElements',
    './input.view',
    './query-time/input-query-time.view'
], function (Marionette, _, $, CustomElements, InputView, InputQueryTimeView) {

    var InputCollectionView = Marionette.CollectionView.extend({
        getChildView: function (item) {
            switch (item.type) {
                case 'query-time':
                    return InputQueryTimeView;
                case 'text':
                    return InputView;
            }
        },
        turnOnEditing: function () {
            this.children.forEach(function (childView) {
                childView.turnOnEditing();
            });
        },
        turnOffEditing: function () {
            this.children.forEach(function (childView) {
                childView.turnOffEditing();
            });
        },
        revert: function () {
            this.children.forEach(function (childView) {
                childView.revert();
            });
        },
        save: function () {
            this.children.forEach(function (childView) {
                childView.save();
            });
        },
        focus: function () {
            this.children.first().focus();
        }
    });

    return InputCollectionView;
});