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
    'text!./workspace-history.hbs',
    'js/CustomElements'
], function (Marionette, _, $, workspaceHistoryTemplate, CustomElements) {
    var selectedVersion;

    var WorkspaceHistory = Marionette.LayoutView.extend({
        template: workspaceHistoryTemplate,
        tagName: CustomElements.register('workspace-history'),
        modelEvents: {
            'all': 'render'
        },
        events: {
            'click .workspaceHistory-body .workspaceHistory-row': 'clickWorkspace'
        },
        ui: {
        },
        regions: {
        },
        initialize: function(){
        },
        highlightSelectedWorkspace: function(){
            this.$el.find('.workspaceHistory-body .workspaceHistory-row').removeClass('is-selected');
            this.$el.find('[data-id='+selectedVersion+']').addClass('is-selected');
        },
        clickWorkspace: function(event){
            var version = event.currentTarget;
            selectedVersion = version.getAttribute('data-id');
            this.highlightSelectedWorkspace();
            this.showButton();
        },
        showButton: function(){
            this.$el.toggleClass('has-selection',Boolean(selectedVersion));
        }
    });

    return WorkspaceHistory;
});
