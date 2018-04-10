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
    './sort-item.view',
    './sort-item-collection.hbs',
    'js/store'
], function (Marionette, _, $, CustomElements, queryItemView, template, store) {

    return Marionette.CompositeView.extend({
        tagName: CustomElements.register('sort-item-collection'),
        childView: queryItemView,
        template: template,
        childViewContainer: ".sorts",
        initialize: function (options) {
            if (this.collection.length === 0) {
                this.collection.add({
                    attribute: 'title',
                    direction: 'ascending'
                });
            }
        },
        childViewOptions: function (model, index) {
            return {
                collection: this.collection,
                childIndex: index,
                parent: this
            }
        }
    });
});