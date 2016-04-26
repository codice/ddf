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
    'text!./home.hbs',
    'js/CustomElements',
    'js/store',
    'component/home-menu/home-menu.view',
    'component/home-templates/home-templates.view',
    'component/home-items/home-items.view'
], function (wreqr, Marionette, _, $, template, CustomElements, store, HomeMenuView, HomeTemplateView, HomeItemView) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('home'),
        modelEvents: {
        },
        events: {
        },
        ui: {
        },
        regions: {
            menu: '.home-menu',
            templates: '.home-templates',
            items: '.home-items'
        },
        initialize: function(){
            this.listenTo(store.get('router'), 'change', this.handleRoute);
            this.handleRoute();
        },
        handleRoute: function(){
            var router = store.get('router').toJSON();
            if (router.name!=='openWorkspace'){
                this.$el.removeClass('is-hidden');
            } else {
                this.$el.addClass('is-hidden');
            }
        },
        onRender: function(){
            this.menu.show(new HomeMenuView());
            this.templates.show(new HomeTemplateView());
            this.items.show(new HomeItemView());
        }
    });
});
