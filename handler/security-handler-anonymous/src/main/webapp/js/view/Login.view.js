/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define, document, window*/
define([
    'marionette',
    'icanhaz',
    'backbone',
    'underscore',
    'text!templates/login.handlebars',
    'jquery',
    'js/application',
    'purl'
], function(Marionette, ich, Backbone, _, loginTemplate, $) {

    ich.addTemplate('loginTemplate', loginTemplate);

    var Login = {};

    Login.LoginForm = Marionette.ItemView.extend({
        template: 'loginTemplate',
        events: {
            'click .btn-signin': 'logInUser',
            'click .btn-clear': 'clearFields',
            'keypress #username': 'logInEnter',
            'keypress #password': 'logInEnter'
        },
        logInEnter: function(e) {
            if (e.keyCode === 13) {
                this.logInUser();
            }
        },
        logInUser: function() {
            var view = this;
            this.deleteCookie();

            var usernamePasswordJson = {
                username: view.$('#username').val(),
                password: view.$('#password').val()
            };

            $.ajax({
                type: "POST",
                url: document.URL,
                async: false,
                dataType: 'json',
                data: usernamePasswordJson,
                error: function() {
                    view.showErrorText();
                    view.setErrorState();
                },
                success: function() {

                    var prevUrl = decodeURIComponent($.url().param('prevurl'));

                    if (!_.isUndefined(prevUrl)) {
                        window.location.href = prevUrl;
                    } else {
                        document.location.reload();
                    }
                }
            });
        },
        showErrorText: function() {
            this.$('#loginError').show();
        },
        setErrorState: function() {
            this.$('#password').focus(function() {
                this.select();
                }
            );
        },
        clearFields: function() {
            this.$('#username').val('');
            this.$('#password').val('');
            this.$('#loginError').hide();
        },
        deleteCookie: function() {
            document.cookie = 'org.codice.websso.saml.token=; Path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
        }
    });

    return Login;
});