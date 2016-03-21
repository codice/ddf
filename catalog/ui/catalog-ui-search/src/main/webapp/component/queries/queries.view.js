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
    'text!./queries.hbs',
    'js/CustomElements',
    'component/tabs/query/tabs-query.view',
    'component/query-selector/query-selector.view',
    'js/store'
], function (Marionette, _, $, queriesTemplate, CustomElements, QueryTabsView,
             QuerySelectorView, store) {

    var Queries = Marionette.LayoutView.extend({
        setDefaultModel: function(){
            this.model = store.getCurrentWorkspace();
        },
        template: queriesTemplate,
        tagName: CustomElements.register('queries'),
        events: {
            'click .workspaces-details': 'shiftRight',
            'click .workspaces-selector': 'shiftLeft'
        },
        ui: {
        },
        regions: {
            'queriesDetails': '.queries-details',
            'queriesSelector': '.queries-selector'
        },
        initialize: function(options){
            if (options.model === undefined) {
                this.setDefaultModel();
            }
            this.listenTo(store.get('content'), 'change:query', this.changeQuery);
        },
        onRender: function(){
            this.queriesSelector.show(new QuerySelectorView());
            this.changeQuery();
        },
        changeQuery: function(){
            var queryRef = store.getQuery();
            if (queryRef === undefined){
                this.queriesDetails.empty();
            } else {
                this.shiftRight();
                this.queriesDetails.show(new QueryTabsView());
            }
        },
        shiftLeft: function(event){
            this.$el.addClass('shifted-left');
            this.$el.removeClass('shifted-right');
        },
        shiftRight: function(){
            this.$el.addClass('shifted-right');
            this.$el.removeClass('shifted-left');
        }
    });

    return Queries;
});
