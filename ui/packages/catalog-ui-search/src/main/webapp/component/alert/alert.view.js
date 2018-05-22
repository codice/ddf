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
    'marionette',
    'underscore',
    'jquery',
    './alert.hbs',
    'js/CustomElements',
    'component/router/router',
    'component/navigation/alert/navigation.alert.view',
    'component/content/alert/content.alert.view',
    'js/model/Query',
    'js/cql',
    'component/singletons/user-instance',
    'component/alert/alert'
], function (Marionette, _, $, template, CustomElements, router, NavigationView,
             AlertContentView, Query, cql, user, alertInstance) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('alert'),
        modelEvents: {
        },
        events: {
        },
        ui: {
        },
        regions: {
            alertMenu: '.alert-menu',
            alertDetails: '.alert-details'
        },
        initialize: function(){
            this.listenTo(router, 'change', this.handleRoute);
            this.listenTo(alertInstance, 'change:currentAlert', this.onBeforeShow);
        },
        handleRoute: function(){
            const routerJSON = router.toJSON();
            if (routerJSON.name === 'openAlert'){
                var alertId = routerJSON.args[0];
                var alert = user.get('user').get('preferences').get('alerts').get(alertId);
                if (!alert) {
                    router.notFound();
                } else {
                    const queryForMetacards = new Query.Model({
                        cql: cql.write({
                            type: 'OR',
                            filters: alert.get('metacardIds').map(function(metacardId){
                                return {
                                    type: '=',
                                    value: metacardId,
                                    property: '"id"'
                                };
                            }).concat({
                                type: '=',
                                value: '-1',
                                property: '"id"'
                            })
                        }),
                        federation: 'enterprise'
                    });
                    if (alertInstance.get('currentQuery')){
                        alertInstance.get('currentQuery').cancelCurrentSearches();
                    }
                    queryForMetacards.startSearch();
                    alertInstance.set({
                        currentResult: queryForMetacards.get('result'),
                        currentAlert: alert,
                        currentQuery: queryForMetacards
                    });
                }
            }
        },
        onRender: function() {
            this.handleRoute();
        },
        onBeforeShow: function(){
           if (alertInstance.get('currentAlert')) {
               this.showSubViews();
           }
        },
        showSubViews() {
            this.alertMenu.show(new NavigationView());
            this.alertDetails.show(new AlertContentView());
        }
    });
});
