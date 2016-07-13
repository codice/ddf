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
    './query-item.hbs',
    'js/CustomElements',
    'js/store',
    'moment',
    'component/dropdown/dropdown',
    'component/dropdown/query-interactions/dropdown.query-interactions.view',
    'component/query-feed/query-feed.view'
], function (Marionette, _, $, template, CustomElements, store, moment,
             DropdownModel, DropdownQueryInteractionsView, QueryFeedView) {

    return Marionette.LayoutView.extend({
        template: template,
        attributes: function(){
            return {
                'data-queryid': this.model.id
            };
        },
        tagName: CustomElements.register('query-item'),
        modelEvents: {
        },
        events: {
            'click .query-actions': 'editQueryDetails'
        },
        ui: {
        },
        regions: {
            queryFeed: '.details-feed',
            queryActions: '.query-actions'
        },
        initialize: function(options){
            var query = store.getQueryById(this.model.id);
            this.listenTo(query, 'change:title', this.updateQuery);
        },
        updateQuery: function() {
            if (!this.isDestroyed){
                this.render();
            }
        },
        highlight: function(){
            var queryRef = store.getQuery();
            this.$el.removeClass('is-selected');
            if (queryRef !== undefined && queryRef.id === this.model.id){
                this.$el.addClass('is-selected');
            }
        },
        serializeData: function(){
           return this.model.toJSON({
                additionalProperties: ['cid', 'color']
            });
        },
        onRender: function(){
            this.queryFeed.show(new QueryFeedView({
                model: this.model
            }));
            this._queryInteractions = new DropdownModel();
            this.queryActions.show(new DropdownQueryInteractionsView({
                model: this._queryInteractions,
                modelForComponent: this.model
            }));
        },
        hideActions: function(){
            this.$el.addClass('hide-actions');
        }
    });
});
