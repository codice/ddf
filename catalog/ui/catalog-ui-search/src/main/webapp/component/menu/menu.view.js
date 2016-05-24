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
    'marionette',
    'underscore',
    'jquery',
    'text!./menu.hbs',
    'js/CustomElements',
    'component/logo/logo.view',
    'component/content-title/content-title.view',
    'component/content-toolbar/content-toolbar.view',
    'component/details-user/details-user.view',
    'component/details-buttons/details-buttons.view',
    'component/dropdown/dropdown',
    'component/dropdown/notifications/dropdown.notifications.view',
    'wreqr'
], function (Marionette, _, $, template, CustomElements, LogoView, TitleView, ToolbarView,
UserView, ButtonsView, DropdownModel, Notifications, wreqr) {

    return Marionette.LayoutView.extend({
        setDefaultModel: function(){
            //override
        },
        template: template,
        tagName: CustomElements.register('menu'),
        events: {
            'click .menu-logo': 'navigateHome'
        },
        regions: {
            logo: '.menu-logo',
            title: '.content-title',
            toolbar: '.content-toolbar',
            user: '.details-user',
            notifications: '.details-notifications'
        },
        initialize: function (options) {
            if (options.model === undefined){
                this.setDefaultModel();
            }
        },
        onBeforeShow: function(){
            this.logo.show(new LogoView());
            this.title.show(new TitleView());
            this.toolbar.show(new ToolbarView());
            this.notifications.show(new Notifications({
                model: new DropdownModel()
            }));
            this.user.show(new UserView());
        },
        navigateHome: function(){
            wreqr.vent.trigger('router:navigate', {
                fragment: 'workspaces',
                options: {
                    trigger: true
                }
            });
        }
    });
});
