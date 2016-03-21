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
    'text!templates/workspace/workspaceIndicator.handlebars',
    'js/CustomElements',
    'component/lightbox/lightbox.view.instance',
    'js/store',
    'component/workspaces/Workspaces.view'
], function (Marionette, _, $, workspaceIndicatorTemplate, CustomElements, lightboxViewInstance, store,
            WorkspacesView) {
    var WorkspaceIndicatorView = Marionette.ItemView.extend({
        template: workspaceIndicatorTemplate,
        tagName: CustomElements.register('workspace-indicator'),
        modelEvents: { 'all': 'rerender' },
        events: { 'click': 'openWorkspaces' },
        initialize: function () {
        },
        serializeData: function () {
            return _.extend(this.model.toJSON(), { currentWorkspace: this.model.getCurrentWorkspaceName() });
        },
        rerender: function () {
            this.render();
        },
        openWorkspaces: function () {
            lightboxViewInstance.model.updateTitle('Workspaces');
            lightboxViewInstance.model.open();
            lightboxViewInstance.lightboxContent.show(new WorkspacesView({
                model: store.get('componentWorkspaces')
            }));
        }
    });
    return WorkspaceIndicatorView;
});
