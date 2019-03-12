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
import { hot } from 'react-hot-loader' 
import LayerItem from '../../container/layer-item/layer-item'
import withListenTo, { WithBackboneProps } from '../../container/backbone-container'

const Sortable = require('sortablejs')
const CustomElements = require('../../../js/CustomElements')
const Component = CustomElements.register('layer-item-collection')

type Props = {
    layerCollection: any
    updateOrdering: any
    focusModel: Backbone.Model
} & WithBackboneProps

type State = {
  layerCollection: any
  updateOrdering: any
  focusModel: Backbone.Model
  sortable?: any
}

const mapPropsToState = ({layerCollection, updateOrdering, focusModel}: Props) => {
  return {layerCollection, updateOrdering, focusModel}

}

const LayerItems = (state: State) => {
  const {layerCollection, updateOrdering, focusModel, sortable} = state
  const layers: Backbone.Model[] = layerCollection.models
  return layers.map(layer => {
    return <LayerItem key={layer.id} layer={layer} collection={layerCollection} options={{updateOrdering, focusModel, sortable}}/>
  })
}

class Layers extends React.Component<Props, State> {
  private layers: React.RefObject<HTMLInputElement>;
  constructor(props: Props) {
    super(props)  
    this.layers=React.createRef()
    this.state = mapPropsToState(props)
    this.listenToLayers()
  }

  listenToLayers = () => {
    const { listenTo, layerCollection } = this.props
    listenTo(layerCollection, 'sort remove add', this.handleChange)
  }
  
  handleChange = () => {
    this.setState({...mapPropsToState(this.props)})
  }

  componentDidMount () {
    const {updateOrdering, focusModel} = this.props
    const sortable = Sortable.create(this.layers.current, {
      handle: 'button.layer-rearrange',
      animation: 250,
      draggable: '>*', // TODO: make a PR to sortable so this won't be necessary
      onEnd: () => {
        focusModel.clear()
        updateOrdering()
      },
    })
  
    this.setState({sortable})

}

  render() {
    return <Component ref={this.layers}>{LayerItems(this.state)}</Component>
  }
} 
    
export default hot(module)(withListenTo(Layers))
// /**
//  * Copyright (c) Codice Foundation
//  *
//  * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
//  * version 3 of the License, or any later version.
//  *
//  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
//  * <http://www.gnu.org/licenses/lgpl.html>.
//  *
//  **/
// import * as React from 'react'
// import { hot } from 'react-hot-loader' 
// import LayerItem from '../../container/layer-item/layer-item'

// const Sortable = require('sortablejs')
// const CustomElements = require('../../../js/CustomElements')
// const Component = CustomElements.register('layer-item-collection')

// type Props = {
//     layers: Backbone.Model[]
//     updateOrdering: any
//     focusModel: Backbone.Model
// } 

// type State = {
//   sortable?: any
// }

// const LayerItems = ({props,state}: {props:Props, state: State}) => {
//   const {layers, updateOrdering, focusModel} = props
//   const { sortable} = state
//   return layers.map(layer => {
//     return <LayerItem key={layer.id} layer={layer} options={{updateOrdering, focusModel, sortable}}/>
//   })
// }

// class Layers extends React.Component<Props, State> {
//   private layers: React.RefObject<HTMLInputElement>;
//   constructor(props: Props) {
//     super(props)  
//     this.layers=React.createRef()
//     this.state = {}
//   }

//   componentDidMount () {
//     const {updateOrdering, focusModel} = this.props
//     const sortable = Sortable.create(this.layers.current, {
//       handle: 'button.layer-rearrange',
//       animation: 250,
//       draggable: '>*', // TODO: make a PR to sortable so this won't be necessary
//       onEnd: () => {
//         focusModel.clear()
//         updateOrdering()
//       },
//     })
  
//     this.setState({sortable})

// }

//   render() {
//     return <Component ref={this.layers}>{LayerItems({props:this.props, state:this.state})}</Component>
//   }
// } 
    
// export default hot(module)(Layers)
