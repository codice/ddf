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
    'text!./metacard-menu.hbs',
    'js/CustomElements',
    'js/store',
    'component/metacard/metacard',
    'component/metacard-title/metacard-title.view',
    'component/router/router'
], function (wreqr, Marionette, _, $, template, CustomElements, store, metacardInstance, MetacardTitleView, router) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('metacard-menu'),
        modelEvents: {
        },
        events: {
            'click > .workspace-title': 'goToWorkspace'
        },
        ui: {
        },
        regions: {
            metacardTitle: '.metacard-title'
        },
        initialize: function(){
            this.listenTo(router, 'change', this.handleRoute);
            this.handleRoute();
        },
        handleRoute: function(){
            if (router.toJSON().name === 'openMetacard'){
                this.model = metacardInstance.get('currentMetacard');
                this.render();
            }
        },
        onRender: function(){
            if (this.model){
                this.metacardTitle.show(new MetacardTitleView({
                    model: new Backbone.Collection(this.model)
                }));
            }
        },
        goToWorkspace: function(e){
            var workspaceId = $(e.currentTarget).attr('data-workspaceid');
            wreqr.vent.trigger('router:navigate', {
                fragment: 'workspaces/'+workspaceId,
                options: {
                    trigger: true
                }
            });
        },
        serializeData: function(){
            var currentWorkspace = store.getCurrentWorkspace();
            var resultJSON, workspaceJSON;
            if (this.model){
                resultJSON = this.model.toJSON()
            }
            if (currentWorkspace){
                workspaceJSON = currentWorkspace.toJSON()
            }
            return {
                result: resultJSON,
                workspace: workspaceJSON
            }
        }
    });
});
