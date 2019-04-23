const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const Sortable = require('sortablejs')

import * as React from 'react'
import LayerItem from '../../react-component/layer-item'

const LayerItemView = Marionette.ItemView.extend({
  attributes: function() {
    return {
      'data-id': this.model.id,
    }
  },
  template() {
    return <LayerItem layer={this.model} options={this.options} />
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
