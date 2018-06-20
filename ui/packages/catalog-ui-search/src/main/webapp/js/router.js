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
/*global define, window, setTimeout, location*/
/* eslint-disable no-undefined */
const wreqr = require('wreqr')
const _ = require('underscore');
const $ = require('jquery')
const Backbone = require('backbone')
const Application = require('application')
const router = require('component/router/router')
const RouterView = require('component/router/router.view');
const plugin = require('plugins/router')
// notfound route needs to come at the end otherwise no other routes will work
const routeDefinitions = {
    ...plugin(require('!./router/routes-loader!js/router/routes.js')),
    ...require('!./router/routes-loader!js/router/routes-notfound.js')
}

const initializeRoutes = function(routeDefinitions) {
    Application.App.router.show(new RouterView({
        routeDefinitions
    }), {
        replaceElement: true
    });
}

const onComponentResolution = function(deferred, component) {
    this.component = this.component || new component();
    deferred.resolve(this.component);
}

initializeRoutes(routeDefinitions);

const Router = Backbone.Router.extend({
    preloadRoutes() {
        Object.keys(routeDefinitions).forEach(this.preloadRoute);
    },
    preloadFragment(fragment) {
        this.preloadRoute(this.getRouteNameFromFragment(fragment));
    },
    preloadRoute(routeName) {
        routeDefinitions[routeName].preload();
    },
    getRouteNameFromFragment(fragment) {
        return this.routes[_.find(Object.keys(this.routes), (routePattern) => {
            return this._routeToRegExp(routePattern).test(fragment);
        })];
    },
    routes: Object.keys(routeDefinitions).reduce((routesBlob, key) => {
        const { patterns } = routeDefinitions[key]
        patterns.forEach((pattern) => routesBlob[pattern] = key);
        return routesBlob
    }, {}),
    initialize: function(){
        if (window.location.search.indexOf('lowBandwidth') !== -1) {
            router.set({
                lowBandwidth: true
            });
        }
        this.listenTo(wreqr.vent, 'router:preload', this.handlePreload);
        this.listenTo(wreqr.vent, 'router:navigate', this.handleNavigate);
        this.on('route', this.onRoute, this);
        /*
            HACK:  listeners for the router aren't setup (such as the onRoute or controller)
                until after initialize is done.  SetTimeout (with timeout of 0) pushes this
                navigate onto the end of the current execution queue
            */
        setTimeout(function(){
            var currentFragment = location.hash;
            Backbone.history.fragment = undefined;
            this.navigate(currentFragment, {trigger: true});
        }.bind(this), 0);
    },
    handlePreload({fragment}) {
        this.preloadFragment(fragment);
    },
    handleNavigate: function(args){
        this.navigate(args.fragment, args.options);
    },
    onRoute: function(name, args){
        this.updateRoute(name, _.invert(this.routes)[name], args);
    },
    updateRoute: function(name, path, args){
        router.set({
            name: name,
            path: path,
            args: args
        });
        $(window).trigger('resize');
        wreqr.vent.trigger('resize');
    }
});

module.exports = new Router();
