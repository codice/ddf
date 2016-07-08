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
    'jquery',
    'text!./login-form.hbs',
    'js/CustomElements',
], function (Marionette, $, template, CustomElements) {

    return Marionette.LayoutView.extend({
        events: {
            'submit .login-form' : 'login',
            'click #sign-out': 'logout'
        },
        template: template,
        tagName: CustomElements.register('login-form'),
        logout: function () {
            //this function is only here to handle clearing basic auth credentials
            //if you aren't using basic auth, this shouldn't do anything
            $.ajax({
                type: 'GET',
                url: '/search/catalog/internal/user',
                async: false,
                username: '1',
                password: '1'
            }).then(function () {
                window.location = '/logout/';
            });
        },
        login: function (e) {
            var view = this;
            this.deleteCookie();
            e.preventDefault(); // prevent form submission

            $.ajax({
                type: 'GET',
                url: '/search/catalog/internal/user',
                async: false,
                beforeSend: function (xhr) {
                    var base64 = window.btoa(view.$('#username').val() + ':' + view.$('#password').val());
                    xhr.setRequestHeader('Authorization', 'Basic ' + base64);
                },
                success: function () {
                    document.location.reload();
                },
                error: function () {
                    view.$('#loginError').show();
                    view.$('#password').focus(function () {
                        view.select();
                    });
                }
            });
        },
        deleteCookie: function () {
            document.cookie = 'JSESSIONID=;path=/;domain=;expires=Thu, 01 Jan 1970 00:00:00 GMT;secure';
        }
    });

});
