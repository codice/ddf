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
/*global require*/
const Marionette = require('marionette');
const template = require('./notfound.hbs');
const CustomElements = require('js/CustomElements');
const router = require('component/router/router');
const NavigationView = require('component/navigation/navigation.view');
const NavigatorView = require('component/navigator/navigator.view');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('notfound'),
    modelEvents: {},
    events: {},
    ui: {},
    regions: {
        content: '> .content > .navigator',
        menu: '> .menu'
    },
    initialize: function() {
        this.listenTo(router, 'change', this.handleRoute);
        this.handleRoute();
    },
    handleRoute: function() {
        if (router.toJSON().name === 'notFound') {
            this.$el.removeClass('is-hidden');
        } else {
            this.$el.addClass('is-hidden');
        }
    },
    onBeforeShow: function() {
        this.menu.show(new NavigationView({
            navigationMiddleText: 'Page Not Found',
            navigationMiddleClasses: 'is-bold'
        }));
        this.content.show(new NavigatorView({}));
    },
    serializeData: function() {
        return {
            route: window.location.hash.substring(1)
        };
    }
});