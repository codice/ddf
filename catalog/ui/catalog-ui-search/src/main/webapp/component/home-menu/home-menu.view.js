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
    'text!./home-menu.hbs',
    'js/CustomElements',
    'component/dropdown/dropdown',
    'component/dropdown/login-form/dropdown.login-form.view',
    'component/dropdown/notifications/dropdown.notifications.view',
    'component/tasks/tasks.view'
], function (wreqr, Marionette, _, $, template, CustomElements, DropdownModel, LoginForm, Notifications, Tasks) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('home-menu'),
        regions: {
            'tasks': '.tasks-region',
            'notifications': '.notifications-region',
            'userInfo': '.user-info-region'
        },
        modelEvents: {
        },
        events: {
        },
        ui: {
        },
        initialize: function(){
        },
        onRender: function(){
            this.notifications.show(new Notifications({
                model: new DropdownModel()
            }));

            this.userInfo.show(new LoginForm({
                model: new DropdownModel()
            }));

            this.tasks.show(new Tasks());
        }
    });
});
