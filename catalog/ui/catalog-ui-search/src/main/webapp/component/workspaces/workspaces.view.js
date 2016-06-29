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
    'text!./workspaces.hbs',
    'js/CustomElements',
    'js/store',
    'component/navigation/workspaces/navigation.workspaces.view',
    'component/workspaces-templates/workspaces-templates.view',
    'component/workspaces-items/workspaces-items.view'
], function (wreqr, Marionette, _, $, template, CustomElements, store, WorkspacesMenuView, WorkspacesTemplatesView, WorkspacesItemsView) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('workspaces'),
        modelEvents: {
        },
        events: {
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
            this.listenTo(store.get('router'), 'change', this.handleRoute);
            this.handleRoute();
        },
        handleRoute: function(){
            var router = store.get('router').toJSON();
            if (router.name === 'home' || router.name === 'workspaces'){
                this.$el.removeClass('is-hidden');
            } else {
                this.$el.addClass('is-hidden');
            }
        },
        onRender: function(){
            this.menu.show(new WorkspacesMenuView());
            this.templates.show(new WorkspacesTemplatesView());
            this.items.show(new WorkspacesItemsView());
        },
        handleTemplatesExpand: function(){
            this.$el.addClass('has-templates-expanded');
        },
        handleTemplatesClose: function(){
            this.$el.removeClass('has-templates-expanded');
        }
    });
});
