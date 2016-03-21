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
    'marionette',
    'backbone',
    'jquery',
    'underscore',
    'direction',
    'spin',
    'spinnerConfig',
    'wreqr',
    './filter/FilterLayout.view',
    'text!templates/resultlist/resultListItem.handlebars',
    'text!templates/resultlist/resultList.handlebars',
    'text!templates/resultlist/metacardTable.handlebars',
    'text!templates/resultlist/countlow.handlebars',
    'text!templates/resultlist/counthigh.handlebars',
    'text!templates/resultlist/statusItem.handlebars',
    'text!templates/resultlist/status.handlebars',
    'properties',
    'js/store'
], function (Marionette, Backbone, $, _, dir, Spinner, spinnerConfig, wreqr, FilterLayoutView,
             resultListItemTemplate, resultListTemplate, metacardTableTemplate, countLowTemplate,
             countHighTemplate, statusItemTemplate, statusTemplate, properties, store) {
    'use strict';
    var List = {};
    function throwError(message, name) {
        var error = new Error(message);
        error.name = name || 'Error';
        throw error;
    }
    function toggleSourceCheckbox(evt, sourceId) {
        if ($('.source-toggle:checked, .excluded-source-toggle:checked').length < 1) {
            return evt.preventDefault();
        }
        if (evt.target.checked) {
            wreqr.vent.trigger('facetSelected', {
                fieldName: properties.filters.SOURCE_ID,
                fieldValue: sourceId
            });
        } else {
            wreqr.vent.trigger('facetDeSelected', {
                fieldName: properties.filters.SOURCE_ID,
                fieldValue: sourceId
            });
        }
    }
    List.MetacardRow = Marionette.ItemView.extend({
        tagName: 'tr',
        template: resultListItemTemplate,
        events: {
            'click .metacard-link': 'viewMetacard',
            'click .select-record-checkbox': 'changeRecordSelection'
        },
        resultSelect: false,
        initialize: function () {
            if (this.model.collection.parents && this.model.collection.parents[0] && this.model.collection.parents[0].get('selecting')) {
                this.resultSelect = true;
            }
            this.listenTo(wreqr.vent, 'search:resultsselect', this.resultSelectMode);
            this.listenTo(wreqr.vent, 'search:resultssave', this.resultSaveMode);
        },
        changeRecordSelection: function (e) {
            this.model.set('selectedForSave', e.target.checked, { silent: true });
        },
        resultSelectMode: function () {
            this.resultSelect = true;
            if (!this.isDestroyed) {
                this.render();
            }
        },
        resultSaveMode: function () {
            this.resultSelect = false;
            if (!this.isDestroyed) {
                this.render();
            }
        },
        serializeData: function () {
            //we are overriding this serializeData function to change marionette's behavior
            //previously it was performing a .toJSON on the model which is normally what you want
            //but our model is pretty deep and this was causing some big performance issues
            //so with this change we simply need up adapt our templates to work with backbone
            //objects instead of flat json records
            return _.extend({}, this.model, { resultSelect: this.resultSelect });
        },
        onRender: function () {
            if (this.model.get('context')) {
                this.$el.addClass('selected');
            }
        },
        viewMetacard: function () {
            wreqr.vent.trigger('metacard:selected', dir.forward, this.model);
        }
    });
    List.MetacardTable = Marionette.CompositeView.extend({
        template: metacardTableTemplate,
        childView: List.MetacardRow,
        childViewContainer: 'tbody',
        childViewOptions: function (model) {
            return { model: model.get('metacard') };
        },
        appendHtml: function (collectionView, childView, index) {
            var childAtIndex;
            if (collectionView.isBuffering) {
                // could just quickly
                // use prepend
                if (index === 0) {
                    collectionView._bufferedChildren.reverse();
                    collectionView._bufferedChildren.push(childView);
                    collectionView._bufferedChildren.reverse();
                    if (collectionView.elBuffer.firstChild) {
                        return collectionView.elBuffer.insertBefore(childView.el, collectionView.elBuffer.firstChild);
                    } else {
                        return collectionView.elBuffer.appendChild(childView.el);
                    }
                } else {
                    // see if there is already
                    // a child at the index
                    childAtIndex = collectionView.$el.children().eq(index);
                    if (childAtIndex.length) {
                        return childAtIndex.before(childView.el);
                    } else {
                        return collectionView.elBuffer.appendChild(childView.el);
                    }
                }
            } else {
                // If we've already rendered the main collection, just
                // append the new items directly into the element.
                var $container = this.getChildViewContainer(collectionView);
                if (index === 0) {
                    $container.empty();
                    return $container.prepend(childView.el);
                } else {
                    // see if there is already
                    // a child at the index
                    childAtIndex = $container.children().eq(index);
                    if (childAtIndex.length) {
                        return childAtIndex.before(childView.el);
                    } else {
                        return $container.append(childView.el);
                    }
                }
            }
        },
        getItemViewContainer: function (containerView) {
            if ('$childViewContainer' in containerView) {
                return containerView.$childViewContainer;
            }
            var container;
            var childViewContainer = Marionette.getOption(containerView, 'childViewContainer');
            if (childViewContainer) {
                var selector = _.isFunction(childViewContainer) ? childViewContainer.call(this) : childViewContainer;
                container = containerView.$(selector);
                if (container.length <= 0) {
                    throwError('The specified `childViewContainer` was not found: ' + containerView.childViewContainer, 'ItemViewContainerMissingError');
                }
            } else {
                container = containerView.$el;
            }
            containerView.$childViewContainer = container;
            return container;
        }
    });
    List.StatusRow = Marionette.ItemView.extend({
        events: { 'click .source-toggle': 'toggleSource' },
        tagName: 'tr',
        template: statusItemTemplate,
        modelEvents: { 'change': 'render' },
        toggleSource: function (e) {
            toggleSourceCheckbox(e, this.model.get('id'));
        }
    });
    List.StatusTable = Marionette.CompositeView.extend({
        template: statusTemplate,
        childView: List.StatusRow,
        childViewContainer: '.included tbody',
        events: {
            'click #status-icon': 'toggleStatus',
            'click .excluded-source-toggle': 'toggleSource'
        },
        collectionEvents: {
            'change': 'render',
            'reset': 'render'
        },
        serializeData: function () {
            var includedSourceIds = this.collection.pluck('id');
            var excludedSources = new Backbone.Collection(this.options.sources.filter(function (source) {
                return !_.contains(includedSourceIds, source.get('id'));
            })).toJSON();
            return { excludedSources: excludedSources };
        },
        toggleStatus: function () {
            this.$('#status-table').toggleClass('shown hidden');
            this.$('#status-icon').toggleClass('fa-caret-down fa-caret-right');
            wreqr.vent.trigger('toggleFilterMenu');
        },
        initFromFilter: function () {
            var showFilter = wreqr.reqres.request('getShowFilterFlag');
            if (showFilter) {
                this.toggleStatus();
                // this should enable it.
                wreqr.vent.trigger('toggleFilterMenu', true);
            }
        },
        onRender: function () {
            this.initFromFilter();
        },
        toggleSource: function (e) {
            toggleSourceCheckbox(e, this.$(e.currentTarget).attr('data-source-id'));
        }
    });
    List.CountView = Marionette.ItemView.extend({
        serializeData: function () {
            var count = 0;
            if (this.model.has('results')) {
                count = this.model.get('results').length;
            }
            var error = false;
            if (this.model.has('status')) {
                error = this.model.get('status').where({ state: 'FAILED' }).length > 0;
            }
            return _.extend(this.model.toJSON(), {
                resultCount: properties.resultCount,
                count: count,
                error: error
            });
        },
        getTemplate: function () {
            if (!_.isUndefined(this.model.get('hits'))) {
                if (!this.model.get('results') || this.model.get('results').length === 0 || properties.resultCount > this.model.get('results').length || properties.resultCount >= this.model.get('hits')) {
                    return countLowTemplate;
                } else {
                    return countHighTemplate;
                }
            } else {
                return false;
            }
        }
    });
    List.MetacardListView = Marionette.LayoutView.extend({
        className: 'slide-animate height-full',
        template: resultListTemplate,
        regions: {
            countRegion: '.result-count',
            listRegion: '#resultList',
            statusRegion: '#result-status-list',
            filterRegion: '.filter-region'
        },
        modelEvents: { 'change': 'render' },
        spinner: new Spinner(spinnerConfig),
        initialize: function () {
            this.listenTo(wreqr.vent, 'search:resultsselect', this.selectingRecords);
            this.listenTo(wreqr.vent, 'search:resultssave', this.saveSelectedRecords);
        },
        onRender: function () {
            var view = this;
            view.listRegion.show(new List.MetacardTable({ collection: view.model.get('results') }));
            if (view.model.get('status')) {
                view.statusRegion.show(new List.StatusTable({
                    collection: view.model.get('status'),
                    sources: store.get('sources')
                }));
            }
            view.countRegion.show(new List.CountView({ model: view.model }));
            view.filterRegion.show(new FilterLayoutView({ model: view.model }));
        },
        onShow: function () {
            this.updateSpinner();
            this.updateScrollbar();
            this.updateScrollPos();
        },
        onDestroy: function () {
            this.spinner.stop();
        },
        updateSpinner: function () {
            if (!_.isUndefined(this.model.get('hits')) || this.model.get('results') && this.model.get('results').length > 0) {
                this.spinner.stop();
            } else {
                this.spinner.spin(this.el);
            }
        },
        updateScrollbar: function () {
            var view = this;
            // defer seems to be necessary for this to update correctly
            _.defer(function () {
                view.$el.perfectScrollbar('update');
            });
        },
        updateScrollPos: function () {
            var view = this;
            _.defer(function () {
                var selected = view.$el.find('.selected');
                var container = view.$el.parent();
                if (selected.length !== 0) {
                    container.scrollTop(selected.offset().top - container.offset().top + container.scrollTop());
                }
            });
        },
        selectingRecords: function () {
            this.model.set('selecting', true, { silent: true });
        },
        saveSelectedRecords: function () {
            this.model.unset('selecting', { silent: true });
            var records = this.model.get('results').where({ 'metacard>selectedForSave': true });
            if (records && records.length) {
                _.each(records, function (record) {
                    record.get('metacard').unset('selectedForSave');
                });
                wreqr.vent.trigger('workspace:saveresults', undefined, records);
            }
        }
    });
    return List;
});
