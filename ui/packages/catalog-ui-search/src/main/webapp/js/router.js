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
const Application = require('application')
const router = require('component/router/router')
const RouterView = require('component/router/router.view');
const plugin = require('plugins/router')

const initializeRoutes = function(routeDefinitions) {
    Application.App.router.show(new RouterView({
        routeDefinitions
    }), {
        replaceElement: true
    });
}

const routeDefinitions = {
    ...plugin({
        openWorkspace: {
            patterns: ['workspaces/:id'],
            component: require('component/content/content.view'),
            menu: {
                component: require('component/workspace-menu/workspace-menu.view')
            }
        },
        home: {
            patterns: ['(?*)', 'workspaces(/)'],
            component: require('component/workspaces/workspaces.view'),
            menu: {
                component: require('component/workspaces-menu/workspaces-menu.view')
            }
        }, 
        openMetacard: {
            patterns: ['metacards/:id'],
            component: require('component/metacard/metacard.view'),
            menu: {
                component: require('component/metacard-menu/metacard-menu.view')
            }
        },
        openAlert: {
            patterns: ['alerts/:id'],
            component: require('component/alert/alert.view'),
            menu: {
                component: require('component/alert-menu/alert-menu.view')
            }
        },
        openIngest: {
            patterns: ['ingest(/)'],
            component: require('component/ingest/ingest.view'),
            menu: {
                classes: 'is-bold',
                text: 'Upload'
            }
        },
        openUpload: {
            patterns: ['uploads/:id'],
            component: require('component/upload/upload.view'),
            menu: {
                component: require('component/upload-menu/upload-menu.view')
            }
        },
        openSources: {
            patterns: ['sources(/)'],
            component: require('component/sources/sources.view'),
            menu: {
                classes: 'is-bold',
                text: 'Sources'
            }
        },
        openAbout: {
            patterns: ['about(/)'],
            component: require('component/about/about.view'),
            menu: {
                classes: 'is-bold',
                text: 'About'
            }
        }
    }),
    // needs to be last based on how backbone router works, otherwise this route always wins
    notFound: {
        patterns: ['*path'],
        component: require('component/notfound/notfound.view'),
        menu: {
            classes: 'is-bold',
            text: 'Page Not Found'
        }
    }    
};

initializeRoutes(routeDefinitions);

const Router = Backbone.Router.extend({
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
