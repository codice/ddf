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
import * as React from 'react'
import withListenTo, { WithBackboneProps } from '../backbone-container'
//import styled from '../../styles/styled-components'
import LayerItemPresentation from '../../presentation/layer-item'

//import Dropdown from '../../presentation/dropdown'
import { hot } from 'react-hot-loader'

// const Span = styled.span`
//   padding-right: 5px;
// `

export interface Order {
  order: number
  isBottom: boolean
  isTop: boolean
}

export interface Visibility {
  alpha: number
  show: boolean
}

export interface Actions {
  updateLayerShow: () => void
  // updateLayerAlpha : () => void
  // moveDown : (e: any) => void
  // moveUp : (e: any) => void
  // onRemove : () => void
}

interface State {
  name: string
  order: Order
  visibility: Visibility
}

type Props = {
  layer: Backbone.Model
} & WithBackboneProps

const mapPropsToState = (props: Props) => {
  const { layer } = props
  const name = layer.get('name')
  const show = layer.get('show')
  const alpha = layer.get('alpha')
  const order = layer.get('order')
  const isBottom = layer.collection.last().id === layer.id
  const isTop = layer.collection.first().id === layer.id

  return {
    name,
    order: { order, isBottom, isTop },
    visibility: { show, alpha },
  }
}

class LayerItem extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = mapPropsToState(props)
    this.listenToLayer()
  }

  listenToLayer = () => {
    const { listenTo, layer } = this.props
    listenTo(layer, 'change:show change:alpha change:order', this.handleChange)
  }

  handleChange = () => {
    this.setState(mapPropsToState(this.props))
  }

  updateLayerShow = () => {
      const show = this.state.visibility.show
    this.props.layer.set('show', !show)
  }
  
  actions = { updateLayerShow: this.updateLayerShow }
    
  render() {
    const props = { ...this.state, actions: this.actions  }
    return <LayerItemPresentation {...props} />
  }
}

export default hot(module)(withListenTo(LayerItem))
