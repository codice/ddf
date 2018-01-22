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
/* jshint -W024*/
/** Main view page for add. **/
define([
    'require',
    'backbone',
    'marionette',
    'icanhaz',
    'underscore',
    'jquery',
    './app-grid/AppCardCollection.view',
    'js/wreqr.js',
    'fileupload',
    'perfectscrollbar'
], function(require, Backbone, Marionette, ich, _, $, AppCardCollectionView, wreqr) {
    "use strict";


    var Model = {};

    var NewApplicationView = Marionette.Layout.extend({
        modelEvents: {
            'change': 'modelChanged'
        },
        initialize: function(options) {
            this.response = options.response;
            this.model = new Backbone.Model({
                isEditMode: false,
                displayMode: 'card'
            });
            this.listenTo(wreqr.vent, 'application:reqestSelection', this.requestSelection);
        },
        requestSelection: function(applicationModel){
            wreqr.vent.trigger('application:selected', applicationModel);
        },
        modelChanged: function(evt){
            this.$(evt.currentTarget).toggleClass('edit-mode', this.model.get('isEditMode'));
            wreqr.vent.trigger('app-grid:edit-mode-toggled', this.model.get('isEditMode'));
        }
    });

    var BOX_LAYOUT = 0;
    var ROW_LAYOUT = 1;
    var ACTIVE_STATE = "ACTIVE";

    // Main layout view for all the applications
    var ApplicationView = Marionette.Layout.extend({
        template: 'applicationGrid',
        tagName: 'div',
        className: 'full-height well',
        regions: {
            applicationGridButtons: '#application-grid-buttons',
            appsgrid: '#apps-grid',
            appsgridInstalled: '.apps-grid-container.installed',
            appsgridNotInstalled: '.apps-grid-container.not-installed',
            applicationViewCancel: '#application-view-cancel'
        },
        events: {
            'click .btn.btn-default.toggle': 'toggleClick',
            'change input[name="options"]':'displayOptionChanged'
        },
        initialize: function (options) {
            var self = this;

            this.modelClass = options.modelClass;
            this.showAddUpgradeBtn = options.showAddUpgradeBtn;
            if(this.modelClass) {
                Model.Collection = new this.modelClass.TreeNodeCollection();

                this.gridState = ACTIVE_STATE;
                this.gridLayout = BOX_LAYOUT;

                this.response = new this.modelClass.Response();
                this.model = Model.Collection;
                this.response.fetch(self.model);
            }
            this.listenTo(wreqr.vent, 'app-grid:edit-mode-toggled', this.toggleEditMode);
        },
        onRender: function () {
            var view = this;

            _.defer(function() {
                view.appsgridInstalled.show(new AppCardCollectionView({collection: view.model, AppShowState: ACTIVE_STATE}));
                view.applicationGridButtons.show(new NewApplicationView({response: view.response}));
                view.$('#application-grid-layout').perfectScrollbar();
            });

            this.listenTo(wreqr.vent, 'toggle:layout', this.toggleView);
        },
        // Toggle used to change the layout of the applications
        toggleClick: function() {
            if(this.gridLayout === BOX_LAYOUT) {
                wreqr.vent.trigger('toggle:layout', ROW_LAYOUT);
            } else {
                wreqr.vent.trigger('toggle:layout', BOX_LAYOUT);
            }
        },
        // Performs action to change css class to alter the view of the applications
        toggleView: function(layout) {
            this.gridLayout = layout;
            if(layout === BOX_LAYOUT) {
                this.$("h2").toggleClass("boxDescription", true);
                $("div.appInfo").toggleClass("box", true);
            } else {
                this.$("h2").toggleClass("boxDescription", false);
                $("div.appInfo").toggleClass("box", false);
            }
        },
        toggleEditMode: function(isEditMode){
            this.$el.toggleClass('edit-mode', isEditMode);
        }
    });

    return ApplicationView;

});
