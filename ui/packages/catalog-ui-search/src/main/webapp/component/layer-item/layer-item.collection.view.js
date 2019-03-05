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
/* global require*/
const _ = require('underscore')
const React = require('react')
const Marionette = require('marionette')
const Backbone = require('backbone')
const $ = require('jquery')
const CustomElements = require('../../js/CustomElements.js')
const Sortable = require('sortablejs')
import LayerItem from '../../react-component/container/layer-item'
var User = require('../../js/model/User.js')

const LayerItemView = Marionette.ItemView.extend({
  template() {
    return <LayerItem layer={this.model} />
  },
})

module.exports = Marionette.CollectionView.extend({
  childView: LayerItemView,
  tagName: CustomElements.register('layer-item-collection'),
  className: 'no-spacing',
  childViewOptions: function() {
    return {
      sortable: this.sortable,
      updateOrdering: this.options.updateOrdering,
      focusModel: this.options.focusModel,
    }
  },
  onBeforeRenderCollection: function() {
    this.sortable = Sortable.create(this.el, {
      handle: 'button.layer-rearrange',
      animation: 250,
      draggable: '>*', // TODO: make a PR to sortable so this won't be necessary
      onEnd: () => {
        this.options.focusModel.clear()
        this.options.updateOrdering()
      },
    })
  },
})
