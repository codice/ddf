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
        'text!templates/search/searchControl.handlebars',
        'direction'
    ],
    function ($, _, Marionette, Backbone, ich, wreqr, searchControlTemplate, dir) {
        "use strict";
        var SearchControl = {};

        ich.addTemplate('searchControlTemplate', searchControlTemplate);

        SearchControl.SearchControlModel = Backbone.Model.extend({
            defaults: {
                back: '',
                forward: '',
                title: 'Search',
                currentState: 'search',
                showChevronBack: true,
                showChevronForward: true
            },
            setInitialState: function() {
                this.set(this.defaults);
            },
            setResultListState: function() {
                this.set({
                    back: 'Search',
                    title: 'Results',
                    forward: '',
                    currentState: 'results'
                });
            },
            setSearchFormState: function() {
                this.set({
                    title: 'Search',
                    forward: 'Results',
                    back: '',
                    currentState: 'search'
                });
            },
            setRecordViewState: function() {
                this.set({
                    title: 'Record',
                    back: 'Results',
                    forward: '',
                    currentState: 'record'
                });
            }
        });

        SearchControl.SearchControlView = Marionette.ItemView.extend({
            template : 'searchControlTemplate',
            model: new SearchControl.SearchControlModel(),
            events: {
                'click .back': 'action',
                'click .forward': 'action'
            },

            modelEvents: {
                'change': 'render'
            },

            initialize: function() {
                this.setupEvents();
                wreqr.vent.on('workspace:tabshown', _.bind(this.setupEvents, this));
            },

            setupEvents: function(tabHash) {
                if(tabHash === '#search') {
                    wreqr.vent.on('metacard:selected', _.bind(this.model.setRecordViewState, this.model));
                    wreqr.vent.on('search:results', _.bind(this.model.setResultListState, this.model));
                    wreqr.vent.on('search:show', _.bind(this.model.setSearchFormState, this.model));
                    wreqr.vent.on('search:clear', _.bind(this.model.setInitialState, this.model));
                } else {
                    wreqr.vent.off('metacard:selected', _.bind(this.model.setRecordViewState, this.model));
                    wreqr.vent.off('search:results', _.bind(this.model.setResultListState, this.model));
                    wreqr.vent.off('search:show', _.bind(this.model.setSearchFormState, this.model));
                    wreqr.vent.off('search:clear', _.bind(this.model.setInitialState, this.model));
                }
            },

            action: function (e) {
                var state = this.model.get('currentState');
                var id = e.target.id;
                switch(state) {
                    case 'search':
                        if(id === 'Results') {
                            wreqr.vent.trigger('search:results', dir.forward);
                        }
                        break;
                    case 'results':
                        if(id === 'Search') {
                            wreqr.vent.trigger('search:show', dir.backward);
                        }
                        break;
                    case 'record':
                        if(id === 'Results') {
                            wreqr.vent.trigger('search:results', dir.backward);
                        }
                        break;
                }
            }
        });

        return SearchControl;
});
