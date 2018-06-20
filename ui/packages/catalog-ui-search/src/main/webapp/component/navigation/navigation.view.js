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
var Marionette = require('marionette');
var template = require('./navigation.hbs');
var CustomElements = require('CustomElements');
var NavigationLeftView = require('component/navigation-left/navigation-left.view');
var NavigationMiddleView = require('component/navigation-middle/navigation-middle.view');
var NavigationRightView = require('component/navigation-right/navigation-right.view');
var store = require('js/store');
var wreqr = require('wreqr');
var sources = require('component/singletons/sources-instance');
var properties = require('properties');
const router = require('component/router/router');
const RouteView = require('component/route/route.view');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('navigation'),
    events: {
        'mousedown > .cancel-drawing': 'handleCancelDrawing'
    },
    regions: {
        navigationLeft: '.navigation-left',
        navigationMiddle: '.navigation-middle',
        navigationRight: '.navigation-right'
    },
    initialize: function() {
        this.listenTo(store.get('workspaces'), 'change:saved update add remove', this.handleSaved);
        this.listenTo(sources, 'all', this.handleSources);
        this.handleSaved();
        this.handleSources();
        this.handleLogo();
    },
    showNavigationMiddle: function(){
        this.navigationMiddle.show(new RouteView({
            routeDefinitions: this.options.routeDefinitions,
            isMenu: true
        }));
    },
    onBeforeShow: function(){
        this.navigationLeft.show(new NavigationLeftView());
        this.showNavigationMiddle();
        this.navigationRight.show(new NavigationRightView());
    },
    handleCancelDrawing: function(e){
        e.stopPropagation();
        wreqr.vent.trigger('search:drawend', store.get('content').get('drawingModel'));
    },
    handleSaved: function(){
        var hasUnsaved = store.get('workspaces').some(function(workspace){
            return !workspace.isSaved();
        });
        this.$el.toggleClass('has-unsaved', hasUnsaved);
    },
    handleSources: function(){
        var hasDown = sources.some(function(source){
            return !source.get('available');
        });
        this.$el.toggleClass('has-unavailable', hasDown);
    },
    handleLogo: function() {
        var hasLogo = properties.showLogo && properties.ui.vendorImage !== "";
        this.$el.toggleClass('has-logo', hasLogo);
    },
    serializeData: function() {
        return {
            middleClasses: this.options.navigationMiddleClasses,
            middleText: this.options.navigationMiddleText
        };
    }
});