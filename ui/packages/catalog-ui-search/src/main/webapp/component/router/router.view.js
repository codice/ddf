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
const routeComponents = {}; 

const getRouteComponent = (routeDefinitions, routeName) => {
    return routeComponents[routeName] || new routeDefinitions[routeName].component();
};

const addToRouteComponents = (routeName, component) => {
    routeComponents[routeName] = routeComponents[routeName] || component;
};

const showRenderedRouteComponent = (routerView, component) => {
    routerView.routerComponent.attachHtml(component, true);
    routerView.routerComponent.attachView(component);
};

const showUnrenderedRouteComponent = (routerView, component) => {
    routerView.routerComponent.show(component, { replaceElement: true, preventDestroy: true });
};

const showRouteComponent = (routerView, component) => {
    component.isRendered === true ? showRenderedRouteComponent(routerView, component) : showUnrenderedRouteComponent(routerView, component);
};

const showRoute = function() {
    const routeName = router.toJSON().name;
    const routeComponent = getRouteComponent(this.options.routeDefinitions, routeName);
    addToRouteComponents(routeName, routeComponent);
    showRouteComponent(this, routeComponent);
}

const RouterView = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('router'),
    regions: {
        routerComponent: '> .router-component' 
    },
    initialize: function () {
        if (this.options.routeDefinitions === undefined) {
            throw "Route definitions must be passed in as an option.";
        }
        this.listenTo(router, 'change', showRoute);
    }
});

module.exports = RouterView;