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
    'text!./workspace-advanced.hbs',
    'js/CustomElements',
], function (Marionette, _, $, workspaceAdvancedTemplate, CustomElements) {

    var WorkspaceAdvanced = Marionette.LayoutView.extend({
        template: workspaceAdvancedTemplate,
        tagName: CustomElements.register('workspace-advanced'),
        modelEvents: {
            'all': 'render'
        },
        events: {
        },
        ui: {
        },
        regions: {
        },
        initialize: function(){
        },
        serializeData: function () {
            return _.extend(this.model.toJSON(), {
                numSavedItems: this.model.get('metacards').length,
                numSearches: this.model.get('searches').length
            });
        },
    });

    return WorkspaceAdvanced;
});
