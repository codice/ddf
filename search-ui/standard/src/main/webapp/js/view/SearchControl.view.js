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
        'icanhaz',
        'wreqr',
        'text!templates/searchControl.handlebars'
    ],
    function ($, _, Marionette, Backbone, ich, wreqr, searchControlTemplate) {
        "use strict";
        var SearchControl = {};

        ich.addTemplate('searchControlTemplate', searchControlTemplate);

        SearchControl.SearchControlModel = Backbone.Model.extend({
            defaults: {
                back: '',
                forward: '',
                title: 'Search'
            },
            currentState: 'search',
            setInitialState: function() {
                this.metacardDetail = undefined;
                this.currentState = 'search';
                var changeObj = {
                    back: '',
                    forward: '',
                    title: 'Search'
                };
                this.set(changeObj);
            },
            setResultListState: function() {
                this.currentState = 'results';
                var changeObj = {
                    back: 'Search',
                    title: 'Results',
                    forward: ''
                };
                if(this.metacardDetail) {
                    changeObj.forward = 'Record';
                }
                this.set(changeObj);
            },
            setSearchFormState: function() {
                this.currentState = 'search';
                var changeObj = {
                    title: 'Search',
                    forward: 'Results',
                    back: ''
                };
                this.set(changeObj);
            },
            setRecordViewState: function() {
                this.metacardDetail = true;
                this.currentState = 'record';
                var changeObj = {
                    title: 'Record',
                    back: 'Results',
                    forward: ''
                };
                this.set(changeObj);
            },
            back: function() {
                if(this.currentState === 'results') {
                    this.setSearchFormState();
                } else if(this.currentState === 'record') {
                    this.setResultListState();
                }
            },
            forward: function() {
                if(this.currentState === 'search') {
                    this.setResultListState();
                } else if(this.currentState === 'results') {
                    this.setRecordViewState();
                }
            }
        });

        SearchControl.SearchControlView = Marionette.ItemView.extend({
            template : 'searchControlTemplate',
            model: new SearchControl.SearchControlModel(),
            events: {
                'click .back': 'back',
                'click .forward': 'forward'
            },

            modelEvents: {
                'change': 'render'
            },

            initialize: function() {
                wreqr.vent.on('metacard:selected', _.bind(this.model.setRecordViewState, this.model));
                wreqr.vent.on('search:results', _.bind(this.model.setResultListState, this.model));
                wreqr.vent.on('search:show', _.bind(this.model.setSearchFormState, this.model));
                wreqr.vent.on('search:clear', _.bind(this.model.setInitialState, this.model));
            },

            back: function () {
                var state = this.model.currentState;
                this.model.back();
                wreqr.vent.trigger('search:back', state);
            },
            forward: function () {
                var state = this.model.currentState;
                this.model.forward();
                wreqr.vent.trigger('search:forward', state);
            }
        });

        return SearchControl;
});
