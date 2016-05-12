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
    'text!./query-selector.hbs',
    'js/CustomElements',
    'js/store',
    'js/model/Query',
    'js/Common'
], function (Marionette, _, $, querySelectorTemplate, CustomElements, store, Query, Common) {

    var QuerySelector = Marionette.LayoutView.extend({
        setDefaultModel: function(){
            this.model = store.getCurrentQueries();
        },
        template: querySelectorTemplate,
        tagName: CustomElements.register('query-selector'),
        modelEvents: {
        },
        events: {
            'click .querySelector-add': 'addQuery',
            'click .querySelector-list .querySelector-queryDetails': 'selectQuery',
            'click .querySelector-queryActive': 'filterQuery'
        },
        ui: {
        },
        regions: {
        },
        initialize: function(options){
            if (options.model === undefined){
                this.setDefaultModel();
            }
            this.listenTo(this.model, 'all', this.render);
            this.listenTo(store.get('content'), 'change:query', this.highlightQuery);
            this.listenTo(store.getFilteredQueries(), 'add', this.filterQueries);
            this.listenTo(store.getFilteredQueries(), 'remove', this.filterQueries);
        },
        addQuery: function(){
            if (this.model.canAddQuery()){
                var newQuery = new Query.Model();
                store.setQueryByReference(newQuery);
            }
        },
        selectQuery: function(event){
            var queryId = event.currentTarget.getAttribute('data-queryId');
            store.setQueryById(queryId);
        },
        highlightQuery: function(){
            var queryRef = store.getQuery();
            this.$el.find('.querySelector-queryDetails').removeClass('is-selected');
            if (queryRef !== undefined){
                this.$el.find('.querySelector-queryDetails[data-queryId="'+queryRef.getId()+'"]').addClass('is-selected');
            }
        },
        filterQuery: function(event){
            var queryId = event.currentTarget.getAttribute('data-queryId');
            store.filterQuery(queryId);
        },
        filterQueries: function(){
            var self = this;
            var filteredQueries = store.getFilteredQueries();
            this.$el.find('.querySelector-queryActive').removeClass('is-filtered');
            filteredQueries.forEach(function(query){
                self.$el.find('.querySelector-queryActive[data-queryId="'+query.getId()+'"]').addClass('is-filtered');
            });
        },
        serializeData: function(){
            var json = this.model.toJSON({
                additionalProperties: ['cid', 'color']
            });
            return json;
        },
        onRender: function(){
            this.handleMaxQueries();
            this.highlightQuery();
        },
        handleMaxQueries: function(){
            this.$el.toggleClass('can-addQuery', this.model.canAddQuery());
        }
    });

    return QuerySelector;
});
