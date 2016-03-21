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
    'js/model/Query'
], function (Marionette, _, $, querySelectorTemplate, CustomElements, store, Query) {

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
            'click .querySelector-queryDetails': 'selectQuery'
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
        },
        addQuery: function(){
            if (this.model.canAddQuery()){
                var newQuery = new Query.Model();
                store.setQueryByReference(newQuery);
            }
        },
        selectQuery: function(event){
            var queryId = event.currentTarget.getAttribute('data-id');
            store.setQueryById(queryId);
        },
        highlightQuery: function(){
            var queryRef = store.getQuery();
            this.$el.find('.querySelector-queryDetails').removeClass('is-selected');
            if (queryRef !== undefined){
                this.$el.find('.querySelector-queryDetails[data-id="'+queryRef._cloneOf+'"]').addClass('is-selected');
            }
        },
        serializeData: function(){
            var json = this.model.toJSON({
                additionalProperties: ['cid']
            });
            json.forEach(function(search){
                var cql = search.cql;
                cql = cql.replace(new RegExp('anyText ILIKE ','g'),'~');
                cql = cql.replace(new RegExp('anyText LIKE ','g'),'');
                cql = cql.replace(new RegExp('AFTER','g'),'>');
                cql = cql.replace(new RegExp('DURING','g'),'BETWEEN');
                search.generatedName = cql;
            });
            return json;
        },
        onRender: function(){
            this.handleMaxQueries();
        },
        handleMaxQueries: function(){
            this.$el.toggleClass('can-addQuery', this.model.canAddQuery());
        }
    });

    return QuerySelector;
});
