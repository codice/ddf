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
/*global define*/
define([
    'wreqr',
    'marionette',
    'underscore',
    'jquery',
    'text!./recent.hbs',
    'js/CustomElements',
    'js/store',
    'component/navigation/recent/navigation.recent.view',
    'component/content/recent/content.recent.view'
], function (wreqr, Marionette, _, $, template, CustomElements, store, NavigationView,
             AlertContentView) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('recent'),
        modelEvents: {
        },
        events: {
        },
        ui: {
        },
        regions: {
            recentMenu: '.recent-menu',
            recentDetails: '.recent-details'
        },
        initialize: function(){
            this.listenTo(store.get('router'), 'change', this.handleRoute);
            this.handleRoute();
        },
        handleRoute: function(){
            var router = store.get('router').toJSON();
            if (router.name === 'openRecent'){
                this.$el.removeClass('is-hidden');
            } else {
                this.$el.addClass('is-hidden');
            }
        },
        onBeforeShow: function(){
            this.recentMenu.show(new NavigationView());
            this.recentDetails.show(new AlertContentView());
        }
    });
});
