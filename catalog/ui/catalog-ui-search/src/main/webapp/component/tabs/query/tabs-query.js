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
    'component/query-old-basic/query-old-basic.view',
    'component/query-updates/query-updates.view',
    'component/query-status/query-status.view'
], function (_, Tabs, store, QueryOldBasicView, QueryUpdatesView, QueryStatusView) {

    var WorkspaceContentTabs = Tabs.extend({
        defaults: {
            tabs: {
                'Basic': QueryOldBasicView,
                'Advanced': QueryOldBasicView,
                'Preview': QueryOldBasicView,
                'Updates': QueryUpdatesView,
                'Status': QueryStatusView
            }
        },
        getAssociatedQuery: function(){
            return store.getQuery();
        }
    });

    return WorkspaceContentTabs;
});