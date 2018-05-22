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
    './workspaces.hbs',
    'js/CustomElements',
    'component/router/router',
    'component/navigation/workspaces/navigation.workspaces.view',
    'component/workspaces-templates/workspaces-templates.view',
    'component/workspaces-items/workspaces-items.view',
    'js/store'
], function (wreqr, Marionette, _, $, template, CustomElements, router, WorkspacesMenuView, 
    WorkspacesTemplatesView, WorkspacesItemsView, store) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('workspaces'),
        modelEvents: {
        },
        events: {
            'click > .home-save': 'handleSave'
        },
        childEvents: {
            'homeTemplates:expand' : 'handleTemplatesExpand',
            'homeTemplates:close' : 'handleTemplatesClose'
        },
        ui: {
        },
        regions: {
            menu: '.home-menu',
            templates: '.home-templates',
            items: '.home-items'
        },
        initialize: function(){
            this.listenTo(router, 'change', this.handleRoute);
            this.listenTo(store.get('workspaces'), 'change:saved update add remove', this.handleSaved);
            this.handleSaved();
        },
        handleRoute: function(){
            var routerName = router.toJSON().name;
            if (routerName=== 'home' || routerName === 'workspaces'){
                this.focus();
            }
        },
        focus: function(){
            this.templates.currentView.focus();
        },
        onRender: function(){
            this.menu.show(new WorkspacesMenuView());
            this.templates.show(new WorkspacesTemplatesView());
            this.items.show(new WorkspacesItemsView());
            this.handleRoute();
        },
        handleTemplatesExpand: function(){
            this.$el.addClass('has-templates-expanded');
        },
        handleTemplatesClose: function(){
            this.$el.removeClass('has-templates-expanded');
        },
        handleSaved: function(){
            var hasUnsaved = store.get('workspaces').find(function(workspace){
                return !workspace.isSaved();
            });
            this.$el.toggleClass('is-saved', !hasUnsaved);
            this.$el.find('> .home-save').attr('tabindex', !hasUnsaved ? -1 : null);
        },
        handleSave: function(){
            store.get('workspaces').saveAll();
        }
    });
});
