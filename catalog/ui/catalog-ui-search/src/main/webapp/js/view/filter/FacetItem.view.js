/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
define([
    'underscore',
    'backbone',
    'marionette',
    'wreqr',
    'text!templates/filter/facet.item.handlebars'
], function (_, Backbone, Marionette, wreqr, facetItemTemplate) {
    'use strict';
    var FacetItemView = Marionette.ItemView.extend({
        template: facetItemTemplate,
        tagName: 'div',
        events: {
            'click .toggle-facet': 'toggleFacet',
            'click .any-button': 'anyButtonClicked',
            'click .toggle-button': 'toggleState'
        },
        initialize: function () {
            this.stateModel = new Backbone.Model({ state: this.options.isAny ? 'any' : 'specific' });
            this.listenTo(this.stateModel, 'change', this.render);
        },
        templateHelpers: function () {
            return {
                isAny: this.options.isAny,
                state: this.stateModel.get('state')
            };
        },
        anyButtonClicked: function () {
            wreqr.vent.trigger('anyFacetClicked', this.model.get('fieldName'));
        },
        toggleState: function () {
            this.stateModel.set({ state: this.stateModel.get('state') === 'any' ? 'specific' : 'any' });
        },
        toggleFacet: function (evt) {
            if (evt.target.checked) {
                this.addFacet(evt);
            } else {
                this.removeFacet(evt);
            }
        },
        removeFacet: function (evt) {
            var element = this.$(evt.currentTarget);
            var valueCount = element.attr('data-value-count');
            var fieldValue = element.attr('data-field-value');
            var fieldName = element.attr('data-field-name');
            wreqr.vent.trigger('facetDeSelected', {
                valueCount: valueCount,
                fieldValue: fieldValue,
                fieldName: fieldName
            });
            return false;
        },
        addFacet: function (evt) {
            var element = this.$(evt.currentTarget);
            var valueCount = element.attr('data-value-count');
            var fieldValue = element.attr('data-field-value');
            var fieldName = element.attr('data-field-name');
            wreqr.vent.trigger('facetSelected', {
                valueCount: valueCount,
                fieldValue: fieldValue,
                fieldName: fieldName
            });
            return false;
        }
    });
    return FacetItemView;
});
