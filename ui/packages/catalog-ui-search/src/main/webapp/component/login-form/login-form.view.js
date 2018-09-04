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
/*global define,window,document*/
define([
    'marionette',
    'jquery',
    './login-form.hbs',
    'js/CustomElements',
], function (Marionette, $, template, CustomElements) {

    return Marionette.LayoutView.extend({
        events: {
            'submit .login-form': 'login',
            'keyup .login-form': 'checkSubmit',
            'click #sign-out': 'logout'
        },
        template: template,
        tagName: CustomElements.register('login-form'),
        checkSubmit: function (e) {
          // check if the enter key was pressed
          if (e.which === 13) {
            this.login(e);
          }
        },
        logout: function () {
            //this function is only here to handle clearing basic auth credentials
            //if you aren't using basic auth, this shouldn't do anything
            $.ajax({
                type: 'GET',
                url: './internal/user',
                async: false,
                username: '1',
                password: '1'
            }).then(function () {
                window.location = '../../logout/?prevurl=' + encodeURI(window.location.pathname);
            });
        },
        login: function (e) {
            var view = this;
            e.preventDefault(); // prevent form submission

            $.ajax({
                type: "POST",
                url: "./internal/login",
                data: {
                    "username": view.$('#username').val(),
                    "password": view.$('#password').val(),
                    "prevurl": window.location.href
                },
                async: false,
                error: function () {
                    view.$('#loginError').show();
                    view.$('#password').focus(function () {
                        view.select();
                    });
                },
                success: function () {
                    document.location.reload();
                }
            });
        }
    });

});
