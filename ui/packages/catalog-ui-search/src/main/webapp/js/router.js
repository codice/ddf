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
const store = require('js/store')
const ConfirmationView = require('component/confirmation/confirmation.view')
const Application = require('application')
const ContentView = require('component/content/content.view')
const HomeView = require('component/workspaces/workspaces.view')
const MetacardView = require('component/metacard/metacard.view')
const Query = require('js/model/Query')
const cql = require('js/cql')
const alertInstance = require('component/alert/alert')
const AlertView = require('component/alert/alert.view')
const IngestView = require('component/ingest/ingest.view')
const router = require('component/router/router')
const user = require('component/singletons/user-instance')
const UploadView = require('component/upload/upload.view')
const NavigatorView = require('component/navigator/navigator.view')
const SourcesView = require('component/sources/sources.view')
const AboutView = require('component/about/about.view')
const NotFoundView = require('component/notfound/notfound.view')
const properties = require('properties')
const announcement = require('component/announcement')
const RouterView = require('component/router/router.view');

Application.App.workspaceRegion.show(new RouterView({
    component: ContentView,
    routes: ['openWorkspace']
}));

Application.App.workspacesRegion.show(new RouterView({
    component: HomeView,
    routes: ['workspaces', 'home']
}));

Application.App.metacardRegion.show(new RouterView({
    component: MetacardView,
    routes: ['openMetacard']
}));

Application.App.uploadRegion.show(new RouterView({
    component: UploadView,
    routes: ['openUpload']
}));

Application.App.notFoundRegion.show(new RouterView({
    component: NotFoundView,
    routes: ['notFound']
}));

Application.App.aboutRegion.show(new RouterView({
    component: AboutView,
    routes: ['openAbout']
}));

Application.App.sourcesRegion.show(new RouterView({
    component: SourcesView,
    routes: ['openSources']
}));

Application.App.ingestRegion.show(new RouterView({
    component: IngestView,
    routes: ['openIngest']
}));

Application.App.alertRegion.show(new RouterView({
    component: AlertView,
    routes: ['openAlert']
}));

var Router = Marionette.AppRouter.extend({
    controller: {
            openWorkspace() {},
            home() {},
            workspaces() {},
            openMetacard() {},
            openAlert() {},
            openIngest() {},
            openUpload() {},
            openSources() {},
            openAbout() {},
            notFound() {}
    },
    appRoutes: {
        'workspaces/:id': 'openWorkspace',
        '(?*)': 'home',
        'workspaces(/)': 'workspaces',
        'metacards/:id': 'openMetacard',
        'alerts/:id': 'openAlert',
        'ingest(/)': 'openIngest',
        'uploads/:id': 'openUpload',
        'sources(/)': 'openSources',
        'about(/)': 'openAbout',
        '*path': 'notFound'
    },
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