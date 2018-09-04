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
var template = require('./navigator.hbs');
var wreqr = require('wreqr');
var properties = require('properties');
var store = require('js/store');
var metacard = require('component/metacard/metacard');
var SaveView = require('component/save/workspaces/workspaces-save.view');
var UnsavedIndicatorView = require('component/unsaved-indicator/workspaces/workspaces-unsaved-indicator.view');
var sources = require('component/singletons/sources-instance');
const plugin = require('plugins/navigator');
const $ = require('jquery');

const visitFragment = (fragment) => wreqr.vent.trigger('router:navigate', {
    fragment: fragment,
    options: {
        trigger: true
    }
});

module.exports = plugin(Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('navigator'),
    regions: {
        workspacesIndicator: '.workspaces-indicator',
        workspacesSave: '.workspaces-save',
        extensions: '.navigation-extensions'
    },
    events: {
        'click .navigation-choice': 'handleChoice'
    },
    initialize: function(){
        this.listenTo(store.get('workspaces'), 'change:saved update add remove', this.handleSaved);
        this.listenTo(sources, 'all', this.handleSourcesChange);
        this.handleSaved();
        this.handleSourcesChange();
    },
    onBeforeShow: function(){
        this.workspacesSave.show(new SaveView());
        this.workspacesIndicator.show(new UnsavedIndicatorView());
        const extensions = this.getExtensions();
        if (extensions) {
            this.extensions.show(extensions);
        }
    },
    getExtensions: function(){},
    handleSaved: function(){
        var hasUnsaved = store.get('workspaces').find(function(workspace){
            return !workspace.isSaved();
        });
        this.$el.toggleClass('is-saved', !hasUnsaved);
    },
    handleSourcesChange: function(){
        var hasDown = sources.some(function(source){
            return !source.get('available');
        });
        this.$el.toggleClass('has-unavailable', hasDown);
    },
    handleChoice(e) {
        visitFragment($(e.currentTarget).attr('data-fragment'));
        this.closeSlideout();
    },
    closeSlideout: function() {
        this.$el.trigger('closeSlideout.' + CustomElements.getNamespace());
    },
    serializeData: function() {
        var currentWorkspace = store.getCurrentWorkspace();
        var workspaceJSON;
        if (currentWorkspace) {
            workspaceJSON = currentWorkspace.toJSON();
        }
        var currentMetacard = metacard.get('currentMetacard');
        var metacardJSON;
        if (currentMetacard){
            metacardJSON = currentMetacard.toJSON();
        }
        return {
            properties: properties,
            workspace: workspaceJSON,
            metacard: metacardJSON,
            recent: workspaceJSON || metacardJSON
        };
    }
}));