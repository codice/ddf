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
// const CustomElements = require('../../../js/CustomElements')
// const Component = CustomElements.register('layer-item-collection')

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
    return <LayerItem key={layer.id} layer={layer} options={{updateOrdering, focusModel, sortable}}/>
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
    listenTo(layerCollection, 'remove add', this.handleChange)
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
    //  return <Component ref={this.layers} {...this.props}>{LayerItems(this.state)}</Component>
    return <> {LayerItems(this.state)} </>
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
// import withListenTo, {
//   WithBackboneProps,
// } from '../../container/backbone-container'
// const SortableJS = require('sortablejs')
// const CustomElements = require('../../../js/CustomElements')
// const Component = CustomElements.register('layer-item-collection')

// import {
//   SortableContainer,
//   SortableElement,
//   arrayMove,
// } from 'react-sortable-hoc'

// type Props = {
//   layerCollection: any
//   updateOrdering: any
//   focusModel: Backbone.Model
// } & WithBackboneProps

// type State = {
//   layerCollection?: any
//   updateOrdering?: any
//   focusModel?: Backbone.Model
//   sortable?: any
//   items: Backbone.Model[]
// }

// const mapPropsToState = ({
//   layerCollection,
//   updateOrdering,
//   focusModel,
// }: Props) => {
//   return { layerCollection, updateOrdering, focusModel }
// }


// const SortableLayerItem = SortableElement(({ layer, options}: {layer:any, options:any}) => {
//   return <LayerItem
//   layer={layer}
//   options={options}
// />
// } )
// const LayerItems = (state: State) => {
//   const { layerCollection, updateOrdering, focusModel, sortable } = state
//   const layers: Backbone.Model[] = layerCollection.models
//   return layers.map(layer => {
//     return (
//       <LayerItem
//         key={layer.id}
//         layer={layer}
//         options={{ updateOrdering, focusModel, sortable }}
//       />
//     )
//   })
// }
// // @ts-ignore
// const SortableLayerItems = SortableContainer(({items}:{items: any}) => {
//   const {  updateOrdering, focusModel, sortable } = items
//   const layers: Backbone.Model[] = items.items
//   return layers.map((layer:any, index:any) => {
//     return (
//       <SortableLayerItem
//         key={`${layer.id}wohho`}
//         index={index}
//         layer={layer}
//         options={{ updateOrdering, focusModel, sortable }}
//       />
//     )
//   })
// })

// // const SortableItem = SortableElement(({ value }:{value:any}) => <li>{value}</li>)

// // const SortableList = SortableContainer(({ items } :{items:any}) => {
// //   return (
// //     <ul>
// //       {items.map((value:any, index:any) => (
// //         <SortableItem key={`item-${index}`} index={index} value={value} />
// //       ))}
// //     </ul>
// //   )
// // })

// class Layers extends React.Component<Props, State> {
//   private layers: React.RefObject<HTMLInputElement>
//   constructor(props: Props) {
//     super(props)
//     this.layers = React.createRef()
//     this.state = {
//       ...mapPropsToState(props),
//       items: this.props.layerCollection.models,
//     }
//     this.listenToLayers()
//   }

//   onSortEnd = ({ oldIndex, newIndex }: {oldIndex:any, newIndex:any}) => {
//     this.setState(({ items }) => ({
//       items: arrayMove(items, oldIndex, newIndex),
//     }))
//   } 

//   listenToLayers = () => {
//     const { listenTo, layerCollection } = this.props
//     listenTo(layerCollection, 'remove add', this.handleChange)
//   }

//   handleChange = () => {
//     this.setState({ ...mapPropsToState(this.props) })
//   }

//   componentDidMount() {
//     const { updateOrdering, focusModel } = this.props
//     const sortable = SortableJS.create(this.layers.current, {
//       handle: '.layer-rearrange',
//       animation: 250,
//       draggable: '>*', // TODO: make a PR to sortable so this won't be necessary
//       onEnd: () => {
//         focusModel.clear()
//         updateOrdering()
//       },
//     })

//     this.setState({ sortable })
//   }

//   render() {
//     return (
//       <>
//         <Component ref={this.layers}>{LayerItems(this.state)}</Component>
//         {/* <SortableList items={this.state.items} onSortEnd={this.onSortEnd} />; */}
//         <SortableLayerItems items={this.state} onSortEnd={this.onSortEnd}/>
//       </>
//     )
//   }
// }

// export default hot(module)(withListenTo(Layers))
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
