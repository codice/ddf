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
var CustomElements = require('CustomElements');
var template = require('./navigation-left.hbs');
var SlideoutLeftViewInstance = require('component/singletons/slideout.left.view-instance.js');
var NavigatorView = require('component/navigator/navigator.view');
var store = require('js/store');
var UnsavedIndicatorView = require('component/unsaved-indicator/workspaces/workspaces-unsaved-indicator.view');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('navigation-left'),
    regions: {
        unsavedIndicator: '.navigation-indicator'
    },
    events: {
        'click': 'toggleNavigator'
    },
    initialize: function(){
        this.listenTo(store.get('workspaces'), 'change:saved update add remove', this.handleSaved);
        this.handleSaved();
    },
    onBeforeShow: function(){
        this.unsavedIndicator.show(new UnsavedIndicatorView());
    },
    toggleNavigator: function(){
        SlideoutLeftViewInstance.updateContent(new NavigatorView());
        SlideoutLeftViewInstance.open();
    },
    handleSaved: function(){
        var hasUnsaved = store.get('workspaces').find(function(workspace){
            return !workspace.isSaved();
        });
        this.$el.toggleClass('is-saved', !hasUnsaved);
    }
});