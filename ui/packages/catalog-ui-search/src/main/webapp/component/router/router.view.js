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

const RouterView = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('router'),
    regions: {
        routerComponent: '> .router-component' 
    },
    initialize: function () {
        if (this.options.routes === undefined || this.options.component === undefined) {
            throw "Route and component must be passed in as options.";
        }
        this.listenTo(router, 'change', this.handleRoute);
        this.handleRoute();
    },
    handleRoute: function(){
        if (this.options.routes.indexOf(router.toJSON().name) !== -1){
            this.showRoute();
            this.$el.parent().removeClass('is-hidden');
        } else {
            this.$el.parent().addClass('is-hidden');
        }
    },
    showRoute: function() {
        if (this.routerComponent.currentView===undefined) {
           this.routerComponent.show(new this.options.component(), { replaceElement: true });
        }
    }
});

module.exports = RouterView;