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
    'text!./workspace-saved.hbs',
    'js/CustomElements',
    'component/result-selector/result-selector.view',
    'js/model/Query',
    'js/store',
    'js/cql',
    'component/confirmation/confirmation.view',
    'js/jquery.whenAll'
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
                        type: 'OR',
                        filters: store.getCurrentWorkspace().get('metacards').map(function (id) {
                            return {
                                type: '=',
                                value: id,
                                property: '"id"'
                            };
                        })
                    })
                });
                $.whenAll.apply(this, query.startSearch()).always(function () {
                    var results = query.get('result').get('results').map(function (result) {
                        return result.get('metacard').get('properties').get('id');
                    });
                    var missingMetacards = store.getCurrentWorkspace().get('metacards').filter(function (id) {
                        return results.indexOf(id) === -1;
                    });
                    if (missingMetacards.length !== 0) {
                        self.listenTo(ConfirmationView.generateConfirmation({
                                prompt: missingMetacards.length + ' metacard(s) unable to be found.  ' +
                                'This could be do to unavailable sources, deletion of the metacard, or lack of permissions to view the metacard.',
                                no: 'Keep',
                                yes: 'Remove'
                            }),
                            'change:choice',
                            function (confirmation) {
                                if (confirmation.get('choice')) {
                                    var currentWorkspace = store.getCurrentWorkspace();
                                    if (currentWorkspace) {
                                        currentWorkspace.set('metacards', _.difference(currentWorkspace.get('metacards'), missingMetacards));
                                    }
                                }
                            });
                    }
                });
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
