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
/*global require*/
var _ = require('underscore');
var Marionette = require('marionette');
var template = require('./map-context-menu.hbs');
var CustomElements = require('js/CustomElements');
var MenuNavigationDecorator = require('decorator/menu-navigation.decorator')
var Decorators = require('decorator/Decorators');
var Clipboard = require('clipboard');
var announcement = require('component/announcement');
var InspectorView = require('component/visualization/inspector/inspector.view');
var HistogramView = require('component/visualization/histogram/histogram.view');
var SelectionInterfaceModel = require('component/selection-interface/selection-interface.model.js');
var lightboxInstance = require('component/lightbox/lightbox.view.instance');

module.exports = Marionette.LayoutView.extend(Decorators.decorate({
    template: template,
    tagName: CustomElements.register('map-context-menu'),
    className: 'is-action-list',
    modelEvents: {},
    events: {
        'click > .interaction-view-details': 'triggerViewDetails',
        'click > .interaction-view-details-selection': 'triggerViewDetailsSelection',
        'click > .interaction-view-histogram-selection': 'triggerHistogramSelection',
        'click > .interaction-view-histogram': 'triggerHistogram',
        'click': 'triggerClick'
    },
    regions: {},
    ui: {},
    initialize: function () {
        this.debounceUpdateSelectionInterface();
        this.selectionInterface = new SelectionInterfaceModel();
        this.listenTo(this.options.mapModel, 'change:clickLat change:clickLon', this.render);
        this.listenTo(this.options.selectionInterface.getSelectedResults(), 'update add remove reset', this.handleSelectionChange);
        this.listenTo(this.options.selectionInterface.getCompleteActiveSearchResults(), 'update add remove reset', this.handleResultsChange);
        this.listenTo(this.selectionInterface.getSelectedResults(), 'update add remove reset', this.updateSelectionInterface);
    },
    debounceUpdateSelectionInterface: function(){
        this.updateSelectionInterface = _.debounce(this.updateSelectionInterface, 200, {leading: false, trailing: true});
    },
    updateSelectionInterface: function() {
        this.options.selectionInterface.clearSelectedResults();
        this.options.selectionInterface.addSelectedResult(this.selectionInterface.getSelectedResults().models);
    },
    handleResultsChange: function() {
        this.$el.toggleClass('has-results', this.options.selectionInterface.getCompleteActiveSearchResults().length > 0);
    },
    handleSelectionChange: function () {
        this.$el.toggleClass('has-selection', this.options.selectionInterface.getSelectedResults().length > 0);
    },
    triggerClick: function () {
        this.$el.trigger('closeDropdown.' + CustomElements.getNamespace());
    },
    triggerHistogram: function() {
        lightboxInstance.model.updateTitle('Histogram');
        lightboxInstance.model.open();
        lightboxInstance.lightboxContent.show(new HistogramView({
            selectionInterface: this.options.selectionInterface
        }));
    },
    triggerHistogramSelection: function() {
        this.stopListening(this.selectionInterface.getSelectedResults(), 'update add remove reset', this.updateSelectionInterface);
        this.selectionInterface.clearSelectedResults();
        this.selectionInterface.addSelectedResult(this.options.selectionInterface.getSelectedResults().models);
        this.selectionInterface.setCompleteActiveSearchResults(this.options.selectionInterface.getSelectedResults());
        lightboxInstance.model.updateTitle('Histogram');
        lightboxInstance.model.open();
        lightboxInstance.lightboxContent.show(new HistogramView({
            selectionInterface: this.selectionInterface
        }));
        this.listenTo(this.selectionInterface.getSelectedResults(), 'update add remove reset', this.updateSelectionInterface);
    },
    triggerViewDetailsSelection: function () {
        lightboxInstance.model.updateTitle('Inspector');
        lightboxInstance.model.open();
        lightboxInstance.lightboxContent.show(new InspectorView({
            selectionInterface: this.options.selectionInterface
        }));
    },
    triggerViewDetails: function () {
        this.stopListening(this.selectionInterface.getSelectedResults(), 'update add remove reset', this.updateSelectionInterface);
        this.selectionInterface.clearSelectedResults();
        this.selectionInterface.addSelectedResult(this.previousHoverModel);
        this.selectionInterface.setCurrentQuery(this.options.selectionInterface.getCurrentQuery());
        lightboxInstance.model.updateTitle('Inspector');
        lightboxInstance.model.open();
        lightboxInstance.lightboxContent.show(new InspectorView({
            selectionInterface: this.selectionInterface
        }));
    },
    onRender: function () {
        this.setupClipboards();
        this.repositionDropdown();
        this.handleTarget();
        this.handleSelectionChange();
        this.handleResultsChange();
        this.keepHoverMetacardAround();
    },
    keepHoverMetacardAround: function () {
        this.previousHoverModel = this.options.mapModel.get('targetMetacard') ||
            this.previousHoverModel; // save in case they hover elsewhere and it's lost, then the user clicks view details
    },
    handleTarget: function () {
        this.$el.toggleClass('has-target', this.options.mapModel.get('target') !== undefined);
    },
    setupClipboards: function () {
        this.attachListenersToClipboard(new Clipboard(this.el.querySelector('.interaction-copy-coordinates')));
        this.attachListenersToClipboard(new Clipboard(this.el.querySelector('.interaction-copy-wkt')));
    },
    attachListenersToClipboard: function (clipboard) {
        clipboard.on('success', function (e) {
            announcement.announce({
                title: 'Copied to clipboard',
                message: e.text,
                type: 'success'
            });
        });
        clipboard.on('error', function (e) {
            announcement.announce({
                title: 'Press Ctrl+C to copy',
                message: e.text,
                type: 'info'
            });
        });
    },
    serializeData: function () {
        var mapModelJSON = this.options.mapModel.toJSON();
        mapModelJSON.selectionCount = this.options.selectionInterface.getSelectedResults().length;
        return mapModelJSON;
    },
    repositionDropdown: function () {
        this.$el.trigger('repositionDropdown.' + CustomElements.getNamespace());
    }
}, MenuNavigationDecorator));