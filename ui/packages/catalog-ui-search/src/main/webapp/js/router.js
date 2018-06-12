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
const $ = require('jquery')
const Backbone = require('backbone')
const Marionette = require('marionette')
const Application = require('application')
const ContentView = require('component/content/content.view')
const HomeView = require('component/workspaces/workspaces.view')
const MetacardView = require('component/metacard/metacard.view')
const AlertView = require('component/alert/alert.view')
const IngestView = require('component/ingest/ingest.view')
const router = require('component/router/router')
const UploadView = require('component/upload/upload.view')
const SourcesView = require('component/sources/sources.view')
const AboutView = require('component/about/about.view')
const NotFoundView = require('component/notfound/notfound.view')
const RouterView = require('component/router/router.view');
const plugin = require('plugins/router')

const openWorkspace = {pattern: 'workspaces/:id'}
Application.App.workspaceRegion.show(new RouterView({
    component: ContentView,
    routes: ['openWorkspace']
}));

const home = {pattern: '(?*)'}
const workspaces = {pattern: 'workspaces(/)'}
Application.App.workspacesRegion.show(new RouterView({
    component: HomeView,
    routes: ['workspaces', 'home']
}));

const openMetacard = {pattern: 'metacards/:id'}
Application.App.metacardRegion.show(new RouterView({
    component: MetacardView,
    routes: ['openMetacard']
}));

const openUpload = {pattern: 'uploads/:id'}
Application.App.uploadRegion.show(new RouterView({
    component: UploadView,
    routes: ['openUpload']
}));

const notFound = {pattern: '*path'}
Application.App.notFoundRegion.show(new RouterView({
    component: NotFoundView,
    routes: ['notFound']
}));

const openAbout = {pattern: 'about(/)'}
Application.App.aboutRegion.show(new RouterView({
    component: AboutView,
    routes: ['openAbout']
}));

const openSources = {pattern: 'sources(/)'}
Application.App.sourcesRegion.show(new RouterView({
    component: SourcesView,
    routes: ['openSources']
}));

const openIngest = {pattern: 'ingest(/)'}
Application.App.ingestRegion.show(new RouterView({
    component: IngestView,
    routes: ['openIngest']
}));

const openAlert = {pattern: 'alerts/:id'}
Application.App.alertRegion.show(new RouterView({
    component: AlertView,
    routes: ['openAlert']
}));

const routes = plugin({
    openWorkspace,
    home,
    workspaces,
    openMetacard,
    openAlert,
    openIngest,
    openUpload,
    openSources,
    openAbout,
    notFound
}, Application.App)

const controller = Object.keys(routes).reduce((route, key) => {
    route[key] = () => {}
    return route
}, {})

const appRoutes = Object.keys(routes).reduce((route, key) => {
    const { pattern } = routes[key]
    route[pattern] = key
    return route
}, {})

const Router = Marionette.AppRouter.extend({
    controller,
    appRoutes,
    initialize: function(){
        if (window.location.search.indexOf('lowBandwidth') !== -1) {
            router.set({
                lowBandwidth: true
            });
        }
        this.listenTo(wreqr.vent, 'router:navigate', this.handleNavigate);
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
    handleNavigate: function(args){
        this.navigate(args.fragment, args.options);
    },
    onRoute: function(name, path, args){
        this.updateRoute(name, path, args);
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
