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
    'component/editor/metacard-basic/metacard-basic.view',
    'component/editor/metacard-advanced/metacard-advanced.view',
    'component/metacard-history/metacard-history.view',
    'component/metacard-associations/metacard-associations.view',
    'component/metacard-quality/metacard-quality.view',
    'component/metacard-actions/metacard-actions.view',
    'component/metacard-archive/metacard-archive.view'
], function (_, Tabs, store, MetacardBasicView, MetacardAdvancedView, MetacardHistoryView,
             MetacardAssociationsView, MetacardQualityView, MetacardActionsView, MetacardArchiveView) {

    return Tabs.extend({
        defaults: {
            tabs: {
                'Summary': MetacardBasicView,
                'Details': MetacardAdvancedView,
                'History': MetacardHistoryView,
                'Associations': MetacardAssociationsView,
                'Quality': MetacardQualityView,
                'Actions': MetacardActionsView,
                'Archive': MetacardArchiveView
            }
        }
    });
});