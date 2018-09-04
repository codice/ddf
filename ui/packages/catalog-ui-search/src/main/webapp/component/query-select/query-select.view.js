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
/*global define, alert*/
define([
    'marionette',
    'underscore',
    'jquery',
    'js/CustomElements',
    'js/store',
    'component/query-item/query-item.collection.view'
], function (Marionette, _, $, CustomElements, store, QueryItemCollectionView) {

    var eventsHash = {
        'click': 'handleClick'
    };

    var namespace = CustomElements.getNamespace();
    var queryItemClickEvent = 'click '+namespace+'query-item';
    eventsHash[queryItemClickEvent] = 'handleQueryItemClick';

    return QueryItemCollectionView.extend({
        className: 'is-query-select composed-menu',
        events: eventsHash,
        onBeforeShow: function(){
            this.handleValue();
        },
        handleQueryItemClick: function(event){
            this.model.set('value', $(event.currentTarget).attr('data-queryid'));
            this.handleValue();
            this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
        },
        handleValue: function(){
            var queryId = this.model.get('value');
            this.$el.find(namespace+'query-item').removeClass('is-selected');
            if (queryId){
                this.$el.find(namespace+'query-item[data-queryid="'+queryId+'"]').addClass('is-selected');
            }
        }
    });
});