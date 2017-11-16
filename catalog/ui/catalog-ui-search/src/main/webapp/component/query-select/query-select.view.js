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
    'component/query-item/query-item.collection.view',
    'decorator/menu-navigation.decorator',
    'decorator/Decorators'
], function (Marionette, _, $, CustomElements, store, QueryItemCollectionView, MenuNavigationDecorator, Decorators) {

    var eventsHash = {
        'click': 'handleClick'
    };

    var namespace = CustomElements.getNamespace();
    var queryItemClickEvent = 'click '+namespace+'query-item';
    eventsHash[queryItemClickEvent] = 'handleQueryItemClick';

    return QueryItemCollectionView.extend(Decorators.decorate({
        className: 'is-query-select is-action-list',
        events: eventsHash,
        onBeforeShow: function(){
            this.handleValue();
        },
        handleQueryItemClick: function(event){
            this.model.set('value', $(event.currentTarget).attr('data-queryid'));
            this.handleValue();
        },
        handleClick: function(){
            this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
        },
        handleValue: function(){
            var queryId = this.model.get('value');
            this.$el.find(namespace+'query-item').removeClass('is-selected');
            if (queryId){
                this.$el.find(namespace+'query-item[data-queryid="'+queryId+'"]').addClass('is-selected');
            }
        },
        onRender: function(){
            if (this.$el.find('.is-active').length === 0){
                this.$el.find(namespace+'query-item').first().addClass('is-active');
            }
        }
    }, MenuNavigationDecorator));
});