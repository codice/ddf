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
    './sort-item.hbs',
    'js/CustomElements',
    'component/singletons/metacard-definitions',
    'component/dropdown/dropdown.view',
    'properties'
], function(Marionette, _, $, template, CustomElements, metacardDefinitions, DropdownView, properties) {

    var blacklist = ['anyText', 'anyGeo'];

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('sort-item'),
        regions: {
            sortAttribute: '.sort-attribute',
            sortDirection: '.sort-direction'
        },
        events: {
            'click .sort-remove': 'removeModel'
        },
        initialize: function(options) {},
        removeModel: function() {
            this.model.destroy();
        },
        onBeforeShow: function() {
            var sortAttributes = metacardDefinitions.sortedMetacardTypes.filter(function(type){
                return !properties.isHidden(type.id);
            }).filter(function(type){
                return !metacardDefinitions.isHiddenType(type.id);
            }).filter(function(type) {
                return blacklist.indexOf(type.id) === -1;
            }).map(function(metacardType) {
                return {
                    label: metacardType.alias || metacardType.id,
                    value: metacardType.id
                };
            });

            this.sortAttribute.show(DropdownView.createSimpleDropdown({
                hasFiltering: true,
                list: sortAttributes,
                defaultSelection: [this.model.get('attribute')]
            }));
            this.sortDirection.show(DropdownView.createSimpleDropdown({
                list: [{
                    label: 'Ascending',
                    value: 'ascending'
                }, {
                    label: 'Descending',
                    value: 'descending'
                }],
                defaultSelection: [this.model.get('direction')]
            }));
            this.listenTo(this.sortAttribute.currentView.model, 'change:value', function(model, attribute) {
                this.model.set('attribute', attribute[0]);
            })
            this.listenTo(this.sortDirection.currentView.model, 'change:value', function(model, direction) {
                this.model.set('direction', direction[0]);
            })
        }
    });
});