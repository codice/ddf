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
    './query-item.hbs',
    'js/CustomElements',
    'component/query-interactions/query-interactions.view',
    'component/query-feed/query-feed.view',
    'component/query-schedule/query-schedule.view',
    'component/query-settings/query-settings.view',
    'component/query-editor/query-editor.view',
    'behaviors/button.behavior',
    'behaviors/dropdown.behavior'
], function (Marionette, _, template, CustomElements,
             QueryInteractionsView, QueryFeedView,
            QueryScheduleView, QuerySettingsView, QueryEditorView) {

    return Marionette.LayoutView.extend({
        template: template,
        attributes: function(){
            return {
                'data-queryid': this.model.id
            };
        },
        behaviors() {
            return {
                button: {},
                dropdown: {
                    dropdowns: [
                        {
                            selector: '.query-actions',
                            view: QueryInteractionsView.extend({
                                behaviors: {
                                    navigation: {}
                                }
                            }),
                            viewOptions: {
                                model: this.options.model
                            }
                        },
                        {
                            selector: '.query-edit',
                            view: QueryEditorView,
                            viewOptions: {
                                model: this.options.model
                            }
                        },
                        {
                            selector: '.query-settings',
                            view: QuerySettingsView,
                            viewOptions: {
                                model: this.options.model
                            }
                        },
                        {
                            selector: '.query-schedule',
                            view: QueryScheduleView,
                            viewOptions: {
                                model: this.options.model
                            }
                        }
                    ]
                }
            };   
        },
        tagName: CustomElements.register('query-item'),
        events: {
            'click .query-run': 'runQuery',
            'click .query-stop': 'stopQuery'
        },
        regions: {
            queryFeed: '.details-feed',
            queryActions: '.query-actions',
            queryEditor: '.query-edit',
            querySettings: '.query-settings',
            querySchedule: '.query-schedule'
        },
        initialize: function(options){
            if (this.model.has('result')) {
                this.startListeningToStatus();
            } else {
                this.listenTo(this.model, 'change:result', this.resultAdded);
            }
            this.listenTo(this.model, 'change:polling', this.handlePolling);
            this.handlePolling();
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
        },
        handlePolling() {
            const polling = this.model.get('polling');
            this.$el.toggleClass('is-polling', polling !== undefined && polling !== false);
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
        }
    });
});
