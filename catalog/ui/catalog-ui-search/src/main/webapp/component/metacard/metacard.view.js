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
    'component/metacard/metacard',
    'component/golden-layout/golden-layout.view'
], function (wreqr, Marionette, _, $, template, CustomElements, router, NavigationView,
            metacardInstance, GoldenLayoutMetacardView) {

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
        },
        initialize: function(){
            this.listenTo(router, 'change', this.handleRoute);
            this.handleRoute();
            this.handleResultChange();
            this.listenTo(metacardInstance, 'change:currentResult', this.handleResultChange);
            this.listenToCurrentMetacard();
        },
        listenToCurrentMetacard: function(){
            /*
                The throttle on the result change should take care of issues with result set merging, but this is a good to have in case
                the timing changes or something else goes awry that we haven't thought of yet.
            */
            this.listenTo(metacardInstance, 'change:currentMetacard', this.handleStatus); 
        },
        handleStatus: function(){
            this.$el.toggleClass('not-found', metacardInstance.get('currentMetacard') === undefined);
            this.$el.toggleClass('is-searching', metacardInstance.get('currentResult').isSearching());
        },
        handleResultChange: function(){
            this.handleStatus();
            this.listenTo(metacardInstance.get('currentResult'), 'sync request error',  _.throttle(this.handleStatus, 60, {leading: false}));
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
            this.detailsTabular.show(new GoldenLayoutMetacardView({
                selectionInterface: metacardInstance,
                configName: 'goldenLayoutMetacard'
            }));
        }
    });
});
