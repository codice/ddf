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
    './workspace-explore.hbs',
    'js/CustomElements',
    'component/query-selector/query-selector.view',
    'js/store',
    'component/lightbox/lightbox.view.instance'
], function (Marionette, _, $, workspaceExploreTemplate, CustomElements, QuerySelectorView,
             store, lightboxViewInstance) {

    var WorkspaceExplore = Marionette.LayoutView.extend({
        setDefaultModel: function(){
            this.model = store.getCurrentWorkspace()
        },
        template: workspaceExploreTemplate,
        tagName: CustomElements.register('workspace-explore'),
        modelEvents: {
        },
        events: {
            'click .querySelector-modal': 'openQueriesModal'
        },
        regions: {
            workspaceExploreQueries: '.workspaceExplore-queries'
        },
        initialize: function (options) {
            if (options.model === undefined){
                this.setDefaultModel();
            }
        },
        onBeforeShow: function(){
            if (this.model) {
                this.workspaceExploreQueries.show(new QuerySelectorView());
            }
        }
    });

    return WorkspaceExplore;
});
