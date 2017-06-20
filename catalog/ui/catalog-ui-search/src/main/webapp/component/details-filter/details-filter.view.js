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
    './details-filter.hbs',
    'js/CustomElements',
    'component/property/property.view',
    'component/property/property'
], function (Marionette, _, $, template, CustomElements, PropertyView, Property) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('details-filter'),
        events: {
            'click > .editor-footer .footer-remove': 'removeFilter',
            'keydown > .editor-properties': 'handleEnter'
        },
        ui: {},
        regions: {
            editorProperties: '.editor-properties'
        },
        initialize: function () {},
        onRender: function () {
            this.editorProperties.show(new PropertyView({
                model: new Property({
                    type: 'STRING',
                    value: [this.model.get('value')],
                    showValidationIssues: false,
                    showLabel: false,
                    placeholder: 'Type to Filter'
                })
            }));
            this.editorProperties.currentView.turnOnEditing();
            this.editorProperties.currentView.turnOnLimitedWidth();
            this.listenTo(this.editorProperties.currentView.model, 'change', this.handleFilterValue);
            this.handleFilterValue();
        },
        handleEnter: function (e) {
            if (e && e.keyCode === 13) {
                this.closeDropdown();
            }
        },
        handleFilterValue: function () {
            this.model.set('value', this.editorProperties.currentView.model.getValue()[0]);
            this.$el.toggleClass('has-filter', this.model.get('value') !== '');
        },
        closeDropdown: function () {
            this.$el.trigger('closeDropdown.' + CustomElements.getNamespace());
        },
        removeFilter: function () {
            this.model.set('value', '');
            this.render();
            this.closeDropdown();
        },
        focus: function () {
            this.$el.find('input').focus();
        }
    });
});