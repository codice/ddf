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
    './metacard.hbs',
    'js/CustomElements',
    'component/router/router',
    'component/navigation/metacard/navigation.metacard.view',
    'component/tabs/metacard/tabs-metacard.view',
    'component/metacard-visual/metacard-visual.view',
    'component/metacard/metacard'
], function (wreqr, Marionette, _, $, template, CustomElements, router, NavigationView,
            MetacardTabularView, MetacardVisualView, metacardInstance) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('metacard'),
        modelEvents: {
        },
        events: {
        },
        ui: {
        },
        regions: {
            metacardMenu: '.metacard-menu',
            detailsTabular: '.details-tabular',
            detailsVisual: '.details-visual'
        },
        initialize: function(){
            this.listenTo(router, 'change', this.handleRoute);
            this.handleRoute();
            this.handleResultChange();
            this.listenTo(metacardInstance, 'change:currentResult', this.handleResultChange);
        },
        handleStatus: function(){
            this.$el.toggleClass('not-found', metacardInstance.get('currentMetacard') === undefined);
            this.$el.toggleClass('is-searching', metacardInstance.get('currentResult').isSearching());
        },
        handleResultChange: function(){
            this.handleStatus();
            this.listenTo(metacardInstance.get('currentResult'), 'sync request error', this.handleStatus);
        },
        handleRoute: function(){
            if (router.toJSON().name === 'openMetacard'){
                this.$el.removeClass('is-hidden');
            } else {
                this.$el.addClass('is-hidden');
            }
        },
        onBeforeShow: function(){
            this.metacardMenu.show(new NavigationView());
            this.detailsTabular.show(new MetacardTabularView({
                selectionInterface: metacardInstance
            }));
            this.detailsVisual.show(new MetacardVisualView());
        }
    });
});
