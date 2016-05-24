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
    'text!./query-sort.hbs',
    'js/CustomElements',
    'js/store'
], function (Marionette, _, $, template, CustomElements, store) {

    var sortFieldToDescription = {
        RELEVANCE: {
            asc: 'Best Text Match',
            desc: 'Best Text Match'
        },
        DISTANCE: {
            asc: 'Shortest Distance',
            desc: 'Furthest Distance'
        },
        modified: {
            asc: 'Earliest Modified',
            desc: 'Latest Modified'
        },
        created: {
            asc: 'Earliest Created',
            desc: 'Latest Created'
        },
        effective: {
            asc: 'Earliest Effective',
            desc: 'Latest Effective'
        }
    };

    var descriptionToSortOrder = {};
    var descriptionToSortField = {};
    for (var sortField in sortFieldToDescription){
        if (sortFieldToDescription.hasOwnProperty(sortField)) {
            for (var sortOrder in sortFieldToDescription[sortField]) {
                if (sortFieldToDescription[sortField].hasOwnProperty(sortOrder)) {
                    descriptionToSortField[sortFieldToDescription[sortField][sortOrder]] = sortField;
                    descriptionToSortOrder[sortFieldToDescription[sortField][sortOrder]] = sortOrder;
                }
            }
        }
    }

    return Marionette.ItemView.extend({
        template: template,
        tagName: CustomElements.register('query-sort'),
        modelEvents: {
            'change': 'render'
        },
        events: {
            'click .choice': 'handleChoice'
        },
        ui: {
        },
        initialize: function(){
        },
        onRender: function(){
            this.handleValue();
        },
        handleValue: function(){
            var value = this.model.get('value');
            this.$el.find('[data-value]').removeClass('is-selected');
            this.$el.find('[data-value="'+
                sortFieldToDescription[value.sortField][value.sortOrder]+
                '"]').addClass('is-selected');
        },
        handleChoice: function(e){
            var value = $(e.currentTarget).attr('data-value');
            this.model.set({
                value: {
                    sortField: descriptionToSortField[value],
                    sortOrder: descriptionToSortOrder[value]
                }
            });
            this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
        },
        serializeData: function(){
            return descriptionToSortOrder;
        }
    });
});
