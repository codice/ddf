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
    './result-item.view',
    'component/result-group/result-group.view',
    'js/store'
], function (Marionette, _, $, CustomElements, childView, groupView, store) {

    return Marionette.CollectionView.extend({
        tagName: CustomElements.register('result-item-collection'),
        getChildView: function(childModel){
            if (childModel.duplicates && !this.options.group){
                return groupView;
            } else {
                return childView;
            }
        },
        className: 'is-list',
        selectionInterface: store,
        initialize: function(options){
            this.selectionInterface = options.selectionInterface || store;
            this.render = _.throttle(this.render, 250);
        },
        onAddChild: function (childView) {
            childView.$el.attr('data-index', this.children.length - 1);
        },
        onRender: function(){
            if (!this.options.group){
                this.selectionInterface.setActiveSearchResults(this.collection);
            } else {
                this.selectionInterface.addToActiveSearchResults(this.collection);
            }
        }
    });
});