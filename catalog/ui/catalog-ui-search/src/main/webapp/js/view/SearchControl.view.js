/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/
define([
    'jquery',
    'underscore',
    'marionette',
    'backbone',
    'wreqr',
    'text!templates/search/searchControl.handlebars',
    'direction'
], function ($, _, Marionette, Backbone, wreqr, searchControlTemplate, dir) {
    'use strict';
    var SearchControl = {};
    SearchControl.SearchControlModel = Backbone.Model.extend({
        defaults: {
            back: '',
            forward: '',
            title: 'Search',
            currentState: 'search',
            showChevronBack: true,
            showChevronForward: true
        },
        setInitialState: function () {
            this.set(this.defaults);
        },
        setResultListState: function () {
            this.set({
                back: 'Search',
                title: 'Results',
                forward: 'Save',
                currentState: 'results',
                showChevronForward: false
            });
        },
        setResultListSelectState: function () {
            this.set({
                back: 'Search',
                title: 'Results',
                forward: 'Done',
                currentState: 'results',
                showChevronForward: false
            });
        },
        setSelectWorkspaceState: function () {
            this.set({
                back: '',
                title: 'Select Workspace',
                forward: '',
                currentState: 'select',
                showChevronForward: false
            });
        },
        setSearchFormState: function () {
            this.set({
                title: 'Search',
                forward: 'Results',
                back: '',
                currentState: 'search',
                showChevronForward: true
            });
        },
        setRecordViewState: function () {
            this.set({
                title: 'Record',
                back: 'Results',
                forward: '',
                currentState: 'record'
            });
        },
        revertToPrevious: function () {
            this.set(this._previousAttributes);
        }
    });
    SearchControl.SearchControlView = Marionette.ItemView.extend({
        template: searchControlTemplate,
        model: new SearchControl.SearchControlModel(),
        events: {
            'click .back': 'action',
            'click .forward': 'action'
        },
        modelEvents: { 'change': 'render' },
        initialize: function () {
            this.setupEvents();
            this.listenTo(wreqr.vent, 'workspace:tabshown', this.setupEvents);
        },
        setupEvents: function (tabHash) {
            if (tabHash === '#search') {
                this.listenTo(wreqr.vent, 'metacard:selected', _.bind(this.model.setRecordViewState, this.model));
                this.listenTo(wreqr.vent, 'search:results', _.bind(this.model.setResultListState, this.model));
                this.listenTo(wreqr.vent, 'search:show', _.bind(this.model.setSearchFormState, this.model));
                this.listenTo(wreqr.vent, 'search:clear', _.bind(this.model.setInitialState, this.model));
                this.listenTo(wreqr.vent, 'search:resultsselect', _.bind(this.model.setResultListSelectState, this.model));
                this.listenTo(wreqr.vent, 'workspace:saveresults', _.bind(this.model.setSelectWorkspaceState, this.model));
                this.listenTo(wreqr.vent, 'workspace:resultssavecancel', _.bind(this.model.revertToPrevious, this.model));
                this.listenTo(wreqr.vent, 'workspace:searchsavecancel', _.bind(this.model.revertToPrevious, this.model));
                this.resetViewState();
            } else {
                this.stopListening(wreqr.vent, 'metacard:selected');
                this.stopListening(wreqr.vent, 'search:results');
                this.stopListening(wreqr.vent, 'search:show');
                this.stopListening(wreqr.vent, 'search:clear');
                this.stopListening(wreqr.vent, 'search:resultsselect');
                this.stopListening(wreqr.vent, 'workspace:saveresults');
                this.stopListening(wreqr.vent, 'workspace:resultssavecancel');
                this.stopListening(wreqr.vent, 'workspace:searchsavecancel');
            }
        },
        resetViewState: function () {
            var state = this.model.get('currentState');
            switch (state) {
            case 'results':
                this.model.setResultListState();
                break;
            }
        },
        action: function (e) {
            var state = this.model.get('currentState');
            var id = e.target.id;
            switch (state) {
            case 'search':
                if (id === 'Results') {
                    wreqr.vent.trigger('search:results', dir.forward);
                }
                break;
            case 'select':
                break;
            case 'results':
                if (id === 'Search') {
                    wreqr.vent.trigger('search:clearfilters');
                    wreqr.vent.trigger('search:show', dir.backward);
                }
                if (id === 'Save') {
                    wreqr.vent.trigger('search:resultsselect');
                }
                if (id === 'Done') {
                    this.model.setResultListState();
                    wreqr.vent.trigger('search:resultssave');
                }
                break;
            case 'record':
                if (id === 'Results') {
                    wreqr.vent.trigger('search:results', dir.backward);
                }
                break;
            }
        }
    });
    return SearchControl;
});
