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
    'backbone',
    'marionette',
    'icanhaz',
    'js/wreqr',
    'jquery',
    'underscore',
    './AppCardItem.view',
    'text!applicationGrid',
    'text!addApplicationCard'
    ],function (Backbone, Marionette, ich, wreqr, $, _, AppCardItemView, applicationGrid, addApplicationCard) {
    "use strict";

    // statics
    var BOX_LAYOUT = 0;
    var ACTIVE_STATE = "ACTIVE";
    var INACTIVE_STATE = "INACTIVE";
    var STOP_STATE = "STOP";

    if(!ich.applicationGrid) {
        ich.addTemplate('applicationGrid', applicationGrid);
    }

    if(!ich.addApplicationCard) {
        ich.addTemplate('addApplicationCard', addApplicationCard);
    }


    var addAppCardItemView = Marionette.ItemView.extend({
        template: 'addApplicationCard'
    });

    // Collection of all the applications
    var AppCardCollectionView = Marionette.CollectionView.extend({
        itemView: AppCardItemView,
        className: 'apps-grid list',
        itemViewOptions: {},
        events: {
            'click .fa.fa-times.stopApp': 'stopPrompt',
            'click .fa.fa-download.startApp': 'startPrompt'
        },
        modelEvents: {
            'change': 'render'
        },
        initialize: function(options) {
            this.AppShowState = options.AppShowState;
            this.listenTo(wreqr.vent, 'toggle:layout', this.toggleLayout);
            this.listenTo(wreqr.vent, 'toggle:state', this.toggleState);
        },

        showCollection: function(){
            var view = this;
            view.collection.each(function(item, index){
                var state = item.get('state');
                if(view.isModelStateMatch(state)){
                    view.addItemView(item, AppCardItemView, index);
                }
            });
            if(this.AppShowState === 'ACTIVE' && this.$('.new-or-update-app').length === 0){
                view.addItemView(new Backbone.Model({}), addAppCardItemView);
            }
        },
        isModelStateMatch: function(state){
            if(_.isArray(this.AppShowState)){
                if(_.contains(this.AppShowState, state)){
                    return true;
                }
            } else if(this.AppShowState === state) {
                return true;
            }
            return false;
        },
        addChildView: function(item, collection, options){
            var state = item.get('state');
            if(this.isModelStateMatch(state)){
                this.closeEmptyView();
                var ItemView = this.getItemView();
                return this.addItemView(item, ItemView, options.index);
            }
        },
        stopPrompt: function() {
            this.toggleState(STOP_STATE);
            this.render();
        },
        startPrompt: function() {
            this.toggleState(INACTIVE_STATE);
            this.render();
        },
        // Changes the css layout
        toggleLayout: function(layout) {
            if(layout === BOX_LAYOUT) {
                this.$("h2").toggleClass("boxDescription", true);
                $("div.appInfo").toggleClass("box", true);
            } else {
                this.$("h2").toggleClass("boxDescription", false);
                $("div.appInfo").toggleClass("box", false);
            }
        },
        // Keeps track of the current view of applications
        toggleState: function(state) {
            if(state === STOP_STATE) {
                this.AppShowState = ACTIVE_STATE;
            } else {
                this.AppShowState = state;
            }
        }
    });

    return AppCardCollectionView;
});