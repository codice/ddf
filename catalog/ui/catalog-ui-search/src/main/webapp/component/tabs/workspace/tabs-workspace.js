/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/
define([
    'underscore',
    '../tabs',
    'js/store',
    'component/workspace-basic/workspace-basic.view',
    'component/workspace-advanced/workspace-advanced.view',
    'component/workspace-history/workspace-history.view',
    'component/workspace-associations/workspace-associations.view',
    'component/workspace-sharing/workspace-sharing.view'
], function (_, Tabs, store, workspaceBasicView, workspaceAdvancedView, workspaceHistoryView,
workspaceAssociationsView, workspaceSharingView) {

    var WorkspaceTabs = Tabs.extend({
        defaults: {
            tabs: {
                'Basic': workspaceBasicView,
                'Advanced': workspaceAdvancedView,
                'History': workspaceHistoryView,
                'Associations': workspaceAssociationsView,
                'Sharing': workspaceSharingView
            },
            workspaceId: undefined
        },
        getAssociatedWorkspace: function() {
            return store.get('workspaces').get('workspaces').get(this.get('workspaceId'));
        }
    });

    return WorkspaceTabs;
});