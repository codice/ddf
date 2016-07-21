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
    'wreqr',
    'marionette',
    'underscore',
    'jquery',
    './workspace-interactions.hbs',
    'js/CustomElements',
    'js/store',
    'component/router/router',
    'component/singletons/user-instance',
    'component/loading/loading.view',
    'component/lightbox/lightbox.view.instance',
    'component/workspace-sharing/workspace-sharing.view'
], function (wreqr, Marionette, _, $, template, CustomElements, store, router, user, LoadingView, lightboxInstance, WorkspaceSharing) {

    return Marionette.ItemView.extend({
        template: template,
        tagName: CustomElements.register('workspace-interactions'),
        modelEvents: {
            'change': 'render'
        },
        events: {
            'click .interaction-subscribe': 'handleSubscribe',
            'click .interaction-unsubscribe': 'handleUnsubscribe',
            'click .interaction-new-tab': 'handleNewTab',
            'click .interaction-share': 'handleShare',
            'click .interaction-duplicate': 'handleDuplicate',
            'click .interaction-trash': 'handleTrash',
            'click .workspace-interaction': 'handleClick',
        },
        ui: {
        },
        initialize: function(){
        },
        onRender: function(){
            this.checkIfSubscribed();
        },
        checkIfSubscribed: function () {
            this.$el.toggleClass('is-subscribed', Boolean(this.model.get('subscribed')));
        },
        handleSubscribe: function () {
            this.model.subscribe();
        },
        handleUnsubscribe: function () {
            this.model.unsubscribe();
        },
        handleNewTab: function () {
            var hasSlash = Boolean(location.href.charAt(location.href.length - 1) === '/');
            window.open(location.href + (hasSlash ? '' : '/') + this.model.id);
        },
        handleShare: function () {
            lightboxInstance.model.updateTitle('Workspace Sharing');
            lightboxInstance.model.open();
            lightboxInstance.lightboxContent.show(new WorkspaceSharing({
                model: this.model
            }));
        },
        handleDuplicate: function () {
            var loadingview = new LoadingView();
            store.get('workspaces').once('sync', function(workspace, resp, options){
                loadingview.remove();
                wreqr.vent.trigger('router:navigate', {
                    fragment: 'workspaces/'+workspace.id,
                    options: {
                        trigger: true
                    }
                });
            });
            store.get('workspaces').duplicateWorkspace(this.model);
        },
        handleTrash: function () {
            var loadingview = new LoadingView();
            store.getWorkspaceById(this.model.id).once('sync', function () {
                wreqr.vent.trigger('router:navigate', {
                    fragment: 'workspaces',
                    options: {
                        trigger: true
                    }
                });
                loadingview.remove();
            });
            store.getWorkspaceById(this.model.id).destroy({
                wait: true
            });
        },
        handleClick: function(){
            this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
        }
    });
});
