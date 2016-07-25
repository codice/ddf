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
    './navigation.hbs',
    'js/CustomElements',
    'component/dropdown/dropdown',
    'component/dropdown/login-form/dropdown.login-form.view',
    'component/dropdown/notifications/dropdown.notifications.view',
    'component/dropdown/alerts/dropdown.alerts.view',
    'component/tasks/tasks.view',
    'component/help/help.view'
], function (wreqr, Marionette, _, $, template, CustomElements, DropdownModel, LoginForm, Notifications,
             Alerts, Tasks, HelpView) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('navigation'),
        regions: {
            notifications: '.notifications-region',
            tasks: '.tasks-region',
            alerts: '.alerts-region',
            userInfo: '.user-info-region',
            navigationMiddle: '.navigation-middle'
        },
        modelEvents: {
        },
        events: {
            'click > .navigation-left > .navigation-home': 'navigateHome',
            'click > .navigation-right > .item-help': 'toggleHelp',
            'mousedown > .navigation-right > .item-help': 'preventPropagation'
        },
        ui: {
        },
        initialize: function(){
        },
        onBeforeShow: function(){
            this.notifications.show(new Notifications({
                model: new DropdownModel()
            }));

            this.alerts.show(new Alerts({
                model: new DropdownModel()
            }));

            this.tasks.show(new Tasks());

            this.userInfo.show(new LoginForm({
                model: new DropdownModel()
            }));
            this.showNavigationMiddle();
        },
        showNavigationMiddle: function(){
            //override in extensions
        },
        navigateHome: function(){
            wreqr.vent.trigger('router:navigate', {
                fragment: 'workspaces',
                options: {
                    trigger: true
                }
            });
        },
        toggleHelp: function(e){
            HelpView.toggleHints();
        },
        preventPropagation: function(e){
            e.stopPropagation();
        }
    });
});
