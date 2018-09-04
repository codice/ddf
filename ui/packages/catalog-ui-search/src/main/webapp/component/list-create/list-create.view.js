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
/*global define, require, module*/
var Marionette = require("marionette");
var _ = require("lodash");
var $ = require("jquery");
var template = require("./list-create.hbs");
var CustomElements = require("js/CustomElements");
var DropdownView = require("component/dropdown/dropdown.view");
var PropertyView = require("component/property/property.view");
var Property = require("component/property/property");
var List = require("js/model/List");
var DropdownView = require("component/dropdown/popout/dropdown.popout.view");
var ListFilterView = require("component/result-filter/list/result-filter.list.view");
var ListEditorView = require("component/list-editor/list-editor.view");
var store = require("js/store");
var ConfirmationView = require('component/confirmation/confirmation.view');

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register("list-create"),
  template: template,
  events: {
    "click > .editor-footer .create-list": "createList",
    "click > .editor-footer .create-list-with-bookmarks": "createListWithBookmarks"
  },
  regions: {
    listEditor: "> .list-editor"
  },
  initialize: function() {
    this.handleBookmarks();
  },
  handleBookmarks: function() {
    this.$el.toggleClass(
      "with-bookmarks",
      _.get(this, ["options", "options", "withBookmarks"]) === true
    );
  },
  onBeforeShow: function() {
    if (!this.isDestroyed) {
      this.showListEditor();
    }
  },
  showListEditor: function() {
    this.listEditor.show(
      new ListEditorView({
        model: new List(),
        showListTemplates: true
      })
    );
  },
  createList: function() {
    this.listEditor.currentView.save();
    store
      .getCurrentWorkspace()
      .get("lists")
      .add(this.listEditor.currentView.model);
    this.onBeforeShow();
  },
  createListWithBookmarks: function() {
    this.listEditor.currentView.save();
    if (this.model.every((result) => {
      return result.matchesCql(this.listEditor.currentView.model.get('list.cql'));
    })) {
      this.listEditor.currentView.model.addBookmarks(this.model.map(function(result){
        return result.get('metacard').id;
      }));
      store
        .getCurrentWorkspace()
        .get("lists")
        .add(this.listEditor.currentView.model, {preventSwitch: true});
        this.onBeforeShow();
    } else {
      this.listenTo(ConfirmationView.generateConfirmation({
          prompt: 'This list\'s filter prevents the result from being in the list.  Create list without result?',
          no: 'Cancel',
          yes: 'Create'
      }),
      'change:choice',
      function(confirmation) {
          if (confirmation.get('choice')) {
            store
              .getCurrentWorkspace()
              .get("lists")
              .add(this.listEditor.currentView.model, {preventSwitch: true});
              this.onBeforeShow();
          }
      }.bind(this));
    }
  }
});
