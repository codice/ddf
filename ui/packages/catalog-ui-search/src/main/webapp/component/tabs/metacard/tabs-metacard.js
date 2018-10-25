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
const _ = require('underscore')
const Tabs = require('../tabs')
const store = require('js/store')
const MetacardBasicView = require('component/editor/metacard-basic/metacard-basic.view')
const MetacardAdvancedView = require('component/editor/metacard-advanced/metacard-advanced.view')
const MetacardHistoryView = require('component/metacard-history/metacard-history.view')
const MetacardAssociationsView = require('component/metacard-associations/metacard-associations.view')
const MetacardQualityView = require('component/metacard-quality/metacard-quality.view')
const MetacardActionsView = require('component/metacard-actions/metacard-actions.view')
const MetacardArchiveView = require('component/metacard-archive/metacard-archive.view')
const MetacardOverwriteView = require('component/metacard-overwrite/metacard-overwrite.view')
const MetacardPreviewView = require('component/metacard-preview/metacard-preview.view')

module.exports = Tabs.extend({
  defaults: {
    tabs: {
      Summary: MetacardBasicView,
      Details: MetacardAdvancedView,
      Preview: MetacardPreviewView,
      History: MetacardHistoryView,
      Associations: MetacardAssociationsView,
      Quality: MetacardQualityView,
      Actions: MetacardActionsView,
      Archive: MetacardArchiveView,
      Overwrite: MetacardOverwriteView,
    },
  },
})
