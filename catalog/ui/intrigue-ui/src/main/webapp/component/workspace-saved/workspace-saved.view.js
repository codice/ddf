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
    './workspace-saved.hbs',
    'js/CustomElements',
    'component/result-selector/result-selector.view',
    'js/model/Query',
    'js/store',
    'js/cql',
    'component/confirmation/confirmation.view'
], function (Marionette, _, $, workspaceSavedTemplate, CustomElements, ResultSelectorView, Query,
             store, cql, ConfirmationView) {

    var WorkspaceSaved = Marionette.LayoutView.extend({
        template: workspaceSavedTemplate,
        tagName: CustomElements.register('workspace-saved'),
        modelEvents: {
        },
        events: {
        },
        regions: {
            resultCollection: '.saved-results'
        },
        initialize: function () {

        },
        onBeforeShow: function(){
            var self = this;
            var isEmpty = store.getCurrentWorkspace().get('metacards').length === 0;
            if (!isEmpty) {
                var query = new Query.Model({
                    cql: cql.write({
                        type: 'AND',
                        filters: [{
                            type: 'OR',
                            filters: store.getCurrentWorkspace().get('metacards').map(function (id) {
                                return {
                                    type: '=',
                                    value: id,
                                    property: '"id"'
                                };
                            })
                        }, {
                            type: 'ILIKE',
                            value: '*',
                            property: '"metacard-tags"'
                        }]
                    })
                });
                query.listenTo(this, 'destroy', query.cancelCurrentSearches);
                query.startSearch();
                this.resultCollection.show(new ResultSelectorView({
                    model: query
                }));
            }
            this.handleEmpty();
        },
        handleEmpty: function(){
            this.$el.toggleClass('is-empty', store.getCurrentWorkspace().get('metacards').length === 0);
        }
    });

    return WorkspaceSaved;
});
