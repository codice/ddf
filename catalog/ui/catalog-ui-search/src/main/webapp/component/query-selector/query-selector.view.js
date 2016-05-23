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
    'component/query-item/query-item.collection.view'
], function (Marionette, _, $, querySelectorTemplate, CustomElements, store, Query, QueryItemCollectionView) {

    var QuerySelector = Marionette.LayoutView.extend({
        setDefaultModel: function(){
            this.model = store.getCurrentQueries();
        },
        template: querySelectorTemplate,
        tagName: CustomElements.register('query-selector'),
        modelEvents: {
        },
        events: function(){
            var eventObj = {
                'click .querySelector-add': 'addQuery'
            };
            eventObj['click .querySelector-list '+CustomElements.getNamespace()+'query-item'] = 'selectQuery';
            return eventObj;
        },
        ui: {
        },
        regions: {
            queryCollection: '.querySelector-list'
        },
        onBeforeShow: function(){
            this.queryCollection.show(new QueryItemCollectionView());
        },
        initialize: function(options){
            if (options.model === undefined){
                this.setDefaultModel();
            }
            this.handleUpdate();
            this.listenTo(this.model, 'add', this.handleUpdate);
            this.listenTo(this.model, 'remove', this.handleUpdate);
            this.listenTo(this.model, 'update', this.handleUpdate);
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
        handleUpdate: function(){
            this.handleMaxQueries();
            this.handleEmptyQueries();
        },
        handleMaxQueries: function(){
            this.$el.toggleClass('can-addQuery', this.model.canAddQuery());
        },
        handleEmptyQueries: function(){
            this.$el.toggleClass('is-empty', this.model.isEmpty());
        }
    });

    return QuerySelector;
});
