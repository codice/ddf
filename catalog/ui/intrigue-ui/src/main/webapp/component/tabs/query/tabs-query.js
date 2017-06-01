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
    'component/query-basic/query-basic.view',
    'component/query-settings/query-settings.view',
    'component/query-status/query-status.view',
    'component/query-schedule/query-schedule.view',
    'component/query-advanced/query-advanced.view'
], function (_, Tabs, store, QueryBasicView, QuerySettingsView, QueryStatusView,
             QueryScheduleView, QueryAdvancedView) {

    var WorkspaceContentTabs = Tabs.extend({
        defaults: {
            tabs: {
                'Basic': QueryBasicView,
                'Advanced': QueryAdvancedView,
                'Settings': QuerySettingsView,
                'Notifications': QueryScheduleView,
                'Status': QueryStatusView
            }
        },
        getAssociatedQuery: function(){
            return store.getQuery();
        }
    });

    return WorkspaceContentTabs;
});