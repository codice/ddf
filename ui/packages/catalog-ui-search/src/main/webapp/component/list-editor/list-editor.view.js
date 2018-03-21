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
var Marionette = require('marionette');
var _ = require('underscore');
var $ = require('jquery');
var template = require('./list-editor.hbs');
var CustomElements = require('js/CustomElements');
require('behaviors/button.behavior');
var DropdownView = require('component/dropdown/dropdown.view');
var PropertyView = require('component/property/property.view');
var Property = require('component/property/property');
var List = require('js/model/List');
var DropdownView = require('component/dropdown/popout/dropdown.popout.view');
var ListFilterView = require('component/result-filter/list/result-filter.list.view');

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('list-editor'),
  template: template,
  events: {
    'click .editor-cancel': 'cancel',
    'click .editor-save': 'save'
  },
  regions: {
    listTitle: '.list-title',
    listCQLSwitch: '.list-limiting-switch',
    listCQL: '.list-limiting',
    listIcon: '.list-icon'
  },
  onBeforeShow: function() {
    this.showListTitle();
    this.showCQLSwitch();
    this.showCQL();
    this.showIcon();
    this.turnOnLimitedWidth();
    this.edit();
  },
  showListTitle: function() {
    this.listTitle.show(
      PropertyView.getPropertyView({
        label: 'Title',
        value: [this.model.get('title')],
        type: 'TEXT'
      })
    );
  },
  showCQLSwitch: function() {
    this.listCQLSwitch.show(
      PropertyView.getPropertyView({
        label: 'Limit based on filter',
        value: [this.model.get('list.cql') !== ''],
        radio: [
          {
            label: 'Yes',
            value: true
          },
          {
            label: 'No',
            value: false
          }
        ]
      })
    );
    this.listenTo(this.listCQLSwitch.currentView.model, 'change:value', this.handleCQLSwitch);
    this.handleCQLSwitch();
  },
  handleCQLSwitch: function() {
    var shouldLimit = this.listCQLSwitch.currentView.model.getValue()[0];
    this.$el.toggleClass('is-limited', shouldLimit);
  },
  showCQL: function() {
    this.listCQL.show(
      DropdownView.createSimpleDropdown({
        componentToShow: ListFilterView,
        defaultSelection: this.model.get('list.cql'),
        leftIcon: 'fa fa-pencil',
        label: 'Edit Filter'
      })
    );
  },
  showIcon: function() {
    this.listIcon.show(
      PropertyView.getPropertyView({
        label: 'Icon',
        value: [this.model.get('list.icon')],
        enum: List.getIconMappingForSelect()
      })
    );
  },
  turnOnLimitedWidth: function() {
    this.regionManager.forEach(function(region) {
      if (region.currentView && region.currentView.turnOnLimitedWidth) {
        region.currentView.turnOnLimitedWidth();
      }
    });
  },
  edit: function() {
    this.$el.addClass('is-editing');
    this.regionManager.forEach(function(region) {
      if (region.currentView && region.currentView.turnOnEditing) {
        region.currentView.turnOnEditing();
      }
    });
    var tabbable = _.filter(this.$el.find('[tabindex], input, button'), function(
      element
    ) {
      return element.offsetParent !== null;
    });
    if (tabbable.length > 0) {
      $(tabbable[0]).focus();
    }
  },
  cancel: function() {
    this.$el.removeClass('is-editing');
    this.onBeforeShow();
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace());
  },
  saveIcon: function() {
    this.model.set('list.icon', this.listIcon.currentView.model.getValue()[0]);
  },
  saveTitle: function() {
    this.model.set('title', this.listTitle.currentView.model.getValue()[0]);
  },
  saveCQL: function() {
    var shouldLimit = this.listCQLSwitch.currentView.model.getValue()[0];
    if (shouldLimit) {
      this.model.set('list.cql', this.listCQL.currentView.model.getValue());
    } else {
      this.model.set('list.cql', '');
    }
  },
  save: function() {
    this.saveTitle();
    this.saveIcon();
    this.saveCQL();
    this.cancel();
  },
  serializeData: function() {
    return this.model.toJSON({
      additionalProperties: ['cid', 'color']
    });
  }
});
