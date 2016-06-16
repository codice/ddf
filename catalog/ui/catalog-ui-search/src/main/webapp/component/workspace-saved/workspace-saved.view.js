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
    'js/cql'
], function (Marionette, _, $, workspaceSavedTemplate, CustomElements, ResultSelectorView, Query, store, cql) {

    var WorkspaceSaved = Marionette.LayoutView.extend({
        template: workspaceSavedTemplate,
        tagName: CustomElements.register('workspace-saved'),
        modelEvents: {
        },
        events: {
        },
        regions: {
            resultCollection: '.workspaceSaved'
        },
        initialize: function () {
        },
        onBeforeShow: function(){
            this.resultCollection.show(new ResultSelectorView({
                model: new Query.Model({
                    cql: cql.write({
                        type: 'OR',
                        filters: store.getCurrentWorkspace().get('metacards').map(function(id){
                            return {
                                type: '=',
                                value: id,
                                property: '"id"'
                            };
                        })
                    })
                })
            }));
        }
    });

    return WorkspaceSaved;
});
