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
var router = require('component/router/router');
var metacard = require('component/metacard/metacard');

module.exports = Marionette.ItemView.extend({
    template: template,
    tagName: CustomElements.register('navigator'),
    events: {
        'click .choice-product': 'handleWorkspaces',
        'click .choice-workspaces': 'handleWorkspaces',
        'click .choice-previous-workspace': 'handlePreviousWorkspace',
        'click .choice-previous-metacard': 'handlePreviousMetacard',
        'click .choice-upload': 'handleUpload',
        'click': 'closeSlideout'
    },
    initialize: function(){
    },
    handleWorkspaces: function(){
        wreqr.vent.trigger('router:navigate', {
            fragment: 'workspaces',
            options: {
                trigger: true
            }
        });
    },
    handlePreviousWorkspace: function(){
        wreqr.vent.trigger('router:navigate', {
            fragment: 'workspaces/'+store.getCurrentWorkspace().id,
            options: {
                trigger: true
            }
        });
    },
    handlePreviousMetacard: function() {
        wreqr.vent.trigger('router:navigate', {
            fragment: 'metacards/'+metacard.get('currentMetacard').get('metacard').id,
            options: {
                trigger: true
            }
        });
    },
    handleUpload: function() {
        wreqr.vent.trigger('router:navigate', {
            fragment: 'ingest',
            options: {
                trigger: true
            }
        });
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
});