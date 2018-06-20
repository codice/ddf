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
/* global require*/
const Marionette = require('marionette');
const template = require('./router.hbs');
const CustomElements = require('js/CustomElements');
const router = require('component/router/router');
const NavigationView = require('component/navigation/navigation.view');
const RouteView = require('component/route/route.view');

const RouterView = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('router'),
    regions: {
        navigation: '> .router-navigation',
        content: '> .router-content'
    },
    initialize: function () {
        if (this.options.routeDefinitions === undefined) {
            throw "Route definitions must be passed in as an option.";
        }
    },
    onBeforeShow: function() {
        this.navigation.show(new NavigationView({
            routeDefinitions: this.options.routeDefinitions
        }));
        this.content.show(new RouteView({
            routeDefinitions: this.options.routeDefinitions
        }));
    }
});

module.exports = RouterView;