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
const routeComponents = {}; 

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
        this.listenTo(router, 'change', this.showRoute);
    },
    showRoute() {
        const routeName = router.toJSON().name;
        const routeComponent = routeComponents[routeName] || new this.options.routeDefinitions[routeName].component();
        routeComponents[routeName] = routeComponents[routeName] || routeComponent;
        if (routeComponent.isRendered === true) {
            this.showRenderedRouteComponent(routeComponent);
        } else {
            this.showUnrenderedRouteComponent(routeComponent);
        }
    },
    showRenderedRouteComponent: function(component) {
        this.content.attachHtml(component, false);
        this.content.attachView(component);
    },
    showUnrenderedRouteComponent: function(component) {
        this.content.show(component, { replaceElement: false, preventDestroy: true });
    },
    onBeforeShow: function() {
        this.navigation.show(new NavigationView({
            routeDefinitions: this.options.routeDefinitions
        }));
    }
});

module.exports = RouterView;