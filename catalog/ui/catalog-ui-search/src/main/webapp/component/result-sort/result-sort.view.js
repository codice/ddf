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
    'marionette',
    'underscore',
    'jquery',
    'text!./result-sort.hbs',
    'js/CustomElements',
    'component/singletons/user-instance',
    'component/sort-item/sort-item.collection.view'
], function (Marionette, _, $, template, CustomElements, user, SortItemCollectionView) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('result-sort'),
        modelEvents: {
            'change': 'render'
        },
        events: {
            'click > .editor-footer .footer-remove': 'removeSort',
            'click > .editor-footer .footer-save': 'saveSort',
            'click > .sort-add': 'addSort'
        },
        ui: {},
        regions: {
            editorProperties: '.editor-properties'
        },
        initialize: function () {
        },
        onRender: function () {
            var resultSort = user.get('user').get('preferences').get('resultSort');
            this.editorProperties.show(new SortItemCollectionView({
                collection: new Backbone.Collection(resultSort)
            }));
        },
        addSort: function () {
            this.editorProperties.currentView.collection.add({
                attribute: 'modified',
                direction: 'descending'
            });
        },
        removeSort: function () {
            user.get('user').get('preferences').set('resultSort', undefined);
            user.get('user').get('preferences').savePreferences();
            this.$el.trigger('closeDropdown.' + CustomElements.getNamespace());
        },
        saveSort: function () {
            var sorting = this.editorProperties.currentView.collection.toJSON();
            user.get('user').get('preferences').set('resultSort', sorting.length === 0 ? undefined : sorting);
            user.get('user').get('preferences').savePreferences();
            this.$el.trigger('closeDropdown.' + CustomElements.getNamespace());
        }
    });
});
