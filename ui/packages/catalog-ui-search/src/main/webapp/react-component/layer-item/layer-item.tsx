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
import * as React from 'react'
import { hot } from 'react-hot-loader'
import { Order, Visibility } from '.'
import withListenTo, {
  WithBackboneProps,
} from '../container/backbone-container'
import LayerItemPresentation from './presentation/layer-item'

const CustomElements = require('../../js/CustomElements')
const Component = CustomElements.register('layer-item')

type State = {
  order: Order
  visibility: Visibility
}

type ContainerProps = {
  layer: Backbone.Model
  options: any
} & WithBackboneProps

const mapPropsToState = (props: ContainerProps) => {
  const { layer } = props
  const show = layer.get('show')
  const alpha = layer.get('alpha')
  const order = layer.get('order')
  const isBottom = layer.collection.last().id === layer.id
  const isTop = layer.collection.first().id === layer.id

  return {
    order: { order, isBottom, isTop },
    visibility: { show, alpha },
  }
}

class LayerItem extends React.Component<ContainerProps, State> {
  _isMounted = false
  constructor(props: ContainerProps) {
    super(props)
    this.state = mapPropsToState(props)
    this.listenToLayer()
  }

  componentDidMount() {
    this._isMounted = true
  }

  componentWillUnmount() {
    this._isMounted = false
  }

  listenToLayer = () => {
    const { listenTo, layer } = this.props
    listenTo(layer, 'change:show change:alpha change:order', this.handleChange)
    listenTo(layer.collection, 'sort remove add', this.handleChange)
  }

  handleChange = () => {
    if (this._isMounted) {
      this.setState(mapPropsToState(this.props))
    }
  }

  updateLayerShow = () => {
    const show = this.state.visibility.show
    this.props.layer.set('show', !show)
  }

  updateLayerAlpha = (e: any) => {
    this.props.layer.set('alpha', e.target.value)
  }

  moveDown = () => {
    const { options, layer } = this.props
    const ordering = options.sortable.toArray()
    const currentIndex = ordering.indexOf(layer.id)
    ordering.splice(currentIndex, 1)
    ordering.splice(currentIndex + 1, 0, layer.id)
    options.sortable.sort(ordering)
    options.focusModel.setDown(layer.id)
    options.updateOrdering()
  }

  moveUp = () => {
    const { options, layer } = this.props
    const ordering = options.sortable.toArray()
    const currentIndex = ordering.indexOf(layer.id)
    ordering.splice(currentIndex - 1, 0, layer.id)
    ordering.splice(currentIndex + 1, 1)
    options.sortable.sort(ordering)
    options.focusModel.setUp(layer.id)
    options.updateOrdering()
  }

  onRemove = () => {
    const { layer } = this.props
    layer.collection.remove(layer)
  }

  _actions = {
    updateLayerShow: this.updateLayerShow,
    updateLayerAlpha: this.updateLayerAlpha,
    moveDown: this.moveDown,
    moveUp: this.moveUp,
    onRemove: this.onRemove,
  }

  render() {
    const { layer } = this.props
    const id = layer.get('id')
    const layerInfo = {
      name: layer.get('name'),
      warning: layer.get('warning'),
      isRemovable: layer.has('userRemovable'),
      id,
    }
    const props = {
      ...this.state,
      layerInfo,
      actions: this._actions,
      options: { focusModel: this.props.options.focusModel },
    }

    return (
      <Component data-id={id}>
        <LayerItemPresentation {...props} />
      </Component>
    )
  }
}

export default hot(module)(withListenTo(LayerItem))
