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
    'component/dropdown/dropdown.view',
    'component/query-feed/query-feed.view',
    'component/dropdown/query-schedule/dropdown.query-schedule.view',
    'component/dropdown/query-status/dropdown.query-status.view',
    'component/dropdown/query-settings/dropdown.query-settings.view',
    'component/dropdown/query-editor/dropdown.query-editor.view',
    'behaviors/button.behavior'
], function (Marionette, _, $, template, CustomElements, store, moment,
             DropdownModel, DropdownQueryInteractionsView, DropdownView, QueryFeedView,
            QueryScheduleView, QueryStatusView, QuerySettingsView, QueryEditorView) {

    return Marionette.LayoutView.extend({
        template: template,
        attributes: function(){
            return {
                'data-queryid': this.model.id
            };
        },
        behaviors: {
            button: {}
        },
        tagName: CustomElements.register('query-item'),
        modelEvents: {
        },
        events: {
            'click .query-run': 'runQuery',
            'click .query-stop': 'stopQuery',
            //'click .query-edit': 'editQuery'
        },
        ui: {
        },
        regions: {
            queryFeed: '.details-feed',
            queryActions: '.query-actions',
            queryEditor: '.query-edit',
            querySettings: '.query-settings',
            queryStatus: '.query-status',
            querySchedule: '.query-schedule'
        },
        initialize: function(options){
            var query = store.getQueryById(this.model.id);
            if (query.has('result')) {
                this.startListeningToStatus();
            } else {
                this.listenTo(query, 'change:result', this.resultAdded);
            }
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
                modelForComponent: this.model,
                dropdownCompanionBehaviors: {
                    navigation: {}
                }
            }));
            this.querySchedule.show(new QueryScheduleView({
                model: new DropdownModel(),
                modelForComponent: this.model
            }));
            this.querySettings.show(new QuerySettingsView({
                model: new DropdownModel(),
                modelForComponent: this.model
            }));
            this.queryEditor.show(new QueryEditorView({
                model: new DropdownModel(),
                modelForComponent: this.model
            }));
            // this.queryStatus.show(new QueryStatusView({
            //     model: new DropdownModel(),
            //     modelForComponent: this.model
            // }));
        },
        hideActions: function(){
            this.$el.addClass('hide-actions');
        },
        handleStatus: function(){
            this.$el.toggleClass('is-searching', this.model.get('result').isSearching());
        },
        resultAdded: function(model) {
            if (model.has('result') && _.isUndefined(model.previous('result'))) {
                this.startListeningToStatus();
            }
        },
        startListeningToStatus: function(){
            this.handleStatus();
            this.listenTo(this.model.get('result'), 'sync request error', this.handleStatus);
        },
        runQuery: function(e){
            this.model.startSearch();
            e.stopPropagation();
        },
        stopQuery: function(e){
            this.model.cancelCurrentSearches();
            e.stopPropagation();
        },
        editQuery: function(e){
            store.setQueryById(this.model.id);
            e.stopPropagation();
        },
        editSchedule: function(e){
            e.stopPropagation(e);
        }
    });
});
