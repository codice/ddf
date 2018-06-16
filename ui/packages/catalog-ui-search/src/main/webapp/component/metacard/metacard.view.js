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
    'component/metacard/metacard',
    'component/golden-layout/golden-layout.view',
    'js/model/Query',
    'js/cql'
], function (wreqr, Marionette, _, $, template, CustomElements, router,
            metacardInstance, GoldenLayoutMetacardView, Query, cql) {

    let queryForMetacard;

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('metacard'),
        regions: {
            detailsTabular: '.details-tabular',
        },
        initialize: function(){
            this.listenTo(router, 'change', this.handleRoute);
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
                const metacardId = router.toJSON().args[0];
                const queryForMetacard = new Query.Model({
                    cql: cql.write({
                        type: 'AND',
                        filters: [{
                            type: '=',
                            value: metacardId,
                            property: '"id"'
                        }, {
                            type: 'ILIKE',
                            value: '*',
                            property: '"metacard-tags"'
                        }]
                    }),
                    federation: 'enterprise'
                });
                if (metacardInstance.get('currentQuery')){
                    metacardInstance.get('currentQuery').cancelCurrentSearches();
                }
                queryForMetacard.startSearch();
                metacardInstance.set({
                    'currentMetacard': undefined,
                    'currentResult': queryForMetacard.get('result'),
                    'currentQuery': queryForMetacard
                });
            }
        },
        onRender: function() {
            this.handleRoute();
            this.handleResultChange();
        },
        onBeforeShow: function(){
            this.detailsTabular.show(new GoldenLayoutMetacardView({
                selectionInterface: metacardInstance,
                configName: 'goldenLayoutMetacard'
            }));
        }
    });
});
