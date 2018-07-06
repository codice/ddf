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
/*global require,window,document*/
var Marionette = require('marionette');
var CustomElements = require('js/CustomElements');
var user = require('component/singletons/user-instance');
var template = require('./user.hbs');
var $ = require('jquery');
var announcement = require('component/announcement');
var logoutActions = require('component/singletons/logout-actions');

module.exports = Marionette.ItemView.extend({
    tagName: CustomElements.register('user'),
    model: user,
    events: {
        'click  #sign-in': 'login',
        'keyup': 'checkSubmit',
        'click #sign-out': 'logout'
    },
    template: template,
    initialize: function() {
        this.handleGuest();
        this.handleIdp();
    },
    handleGuest: function() {
        this.$el.toggleClass('is-guest', this.model.get('user').get('isGuest'));
    },
    handleIdp: function() {
        this.$el.toggleClass('is-idp', logoutActions.isIdp());
    },
    onAttach: function() {
        this.$el.find('#username').focus();
    },
    checkSubmit: function(e) {
        // check if the enter key was pressed
        if (e.which === 13 && $('#username').val() !== '') {
            this.login(e);
        }
    },
    logout: function() {
        //this function is only here to handle clearing basic auth credentials
        //if you aren't using basic auth, this shouldn't do anything
        $.ajax({
            type: 'GET',
            url: '/search/catalog/internal/user',
            async: false,
            username: '1',
            password: '1'
        }).then(function() {
            window.location = '/logout?prevurl=' + encodeURI(window.location.pathname);
        });
    },
    login: function(e) {
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
            customErrorHandling: true,
            error: function () {
                announcement.announce({
                    title: 'Sign In Failed',
                    message: 'Please verify your credentials and attempt to sign in again.',
                    type: 'error'
                });
            },
            success: function () {
                document.location.reload();
            }
        });
    },
    serializeData: function() {
        return {
            user: this.model.toJSON(),
            idp: logoutActions.isIdp()
        };
    }
});