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
const $ = require('jquery');
const wreqr = require('wreqr');
const Marionette = require('marionette');
const template = require('./route.hbs');
const CustomElements = require('js/CustomElements');
const router = require('component/router/router');
const LoadingCompanionView = require('component/loading-companion/loading-companion.view');

const replaceElement = true;

// needed for golden-layout
const triggerResize = () => {
    wreqr.vent.trigger('resize');
    $(window).trigger('resize');
}

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('route'),
    regions: {
        content: '> div'
    },
    initialize: function () {
        if (this.options.routeDefinitions === undefined) {
            throw "Route definitions must be passed in as an option.";
        }
        this.listenTo(router, 'change', this.showRoute);
    },
    showRoute() {
        const routeName = router.toJSON().name;
        if (this.needsFetching(routeName)) {
            LoadingCompanionView.beginLoading(this);
        }
        this.getComponent(routeName).then((component) => {
            if (routeName !== router.toJSON().name) {
                return;
            }
            if (component.isRendered === true) {
                this.showRenderedRouteComponent(component);
            } else {
                this.showUnrenderedRouteComponent(component);
            }
            triggerResize();
            LoadingCompanionView.endLoading(this);
        });
    },
    needsFetching: function(routeName) {
        if (this.options.isMenu) {
            return this.options.routeDefinitions[routeName].menu.component === undefined;
        } else {
            return this.options.routeDefinitions[routeName].component === undefined;
        }
    },
    getComponent: function(routeName) {
        if (this.options.isMenu) {
            return this.options.routeDefinitions[routeName].menu.getComponent();
        } else {
            return this.options.routeDefinitions[routeName].getComponent();
        }
    },
    showRenderedRouteComponent: function(component) {
        this.content.attachHtml(component, replaceElement);
        this.content.attachView(component);
    },
    showUnrenderedRouteComponent: function(component) {
        this.content.show(component, { replaceElement, preventDestroy: true });
    }
});