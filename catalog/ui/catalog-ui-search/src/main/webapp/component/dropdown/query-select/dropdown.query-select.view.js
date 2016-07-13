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
    '../dropdown.view',
    './dropdown.query-select.hbs',
    'component/query-select/query-select.view',
    'component/query-item/query-item.view',
    'js/store'
], function (Marionette, _, $, DropdownView, template, ComponentView, QueryItemView, store) {

    return DropdownView.extend({
        template: template,
        className: 'is-querySelect',
        componentToShow: ComponentView,
        regions: {
            queryItem: '.querySelect-item'
        },
        initializeComponentModel: function(){
            //override if you need more functionality
            this.modelForComponent = this.options.model;
        },
        listenToComponent: function(){
            //override if you need more functionality
        },
        isCentered: true,
        getCenteringElement: function(){
            return this.el;
        },
        hasTail: true,
        onRender: function(){
            DropdownView.prototype.onRender.call(this);
            var queryId = this.model.get('value');
            if (queryId){
                this.queryItem.show(new QueryItemView({
                    model: store.getCurrentQueries().get(queryId)
                }));
                this.$el.addClass('query-selected');
            } else {
                this.$el.removeClass('query-selected');
            }
        }
    });
});
