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
    'icanhaz',
    'wreqr',
    'text!templates/filter/facet.item.handlebars'
],
    function (_, Backbone,Marionette, ich, wreqr, facetItemTemplate) {
        "use strict";


        ich.addTemplate('facetItemTemplate', facetItemTemplate);

        var FacetItemView = Marionette.ItemView.extend({
            template: 'facetItemTemplate',
            tagName: 'div',
            events: {
                'click .toggle-facet': 'toggleFacet'
            },
            toggleFacet: function(evt){
                if (evt.target.checked) {
                    this.addFacet(evt);
                } else {
                    this.removeFacet(evt);
                }
            },
            removeFacet: function(evt){
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
            addFacet: function(evt){

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