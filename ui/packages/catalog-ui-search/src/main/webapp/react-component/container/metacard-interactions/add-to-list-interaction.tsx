import * as React from 'react'
import * as PopoutView from '../../../component/dropdown/popout/dropdown.popout.view'
import * as ResultAddView from '../../../component/result-add/result-add.view'
import { Model } from '.'
import * as store from '../../../js/store'
import MarionetteRegionContainer from '../marionette-region-container'
import { hot } from 'react-hot-loader'

const createAddRemoveRegion = (model: Model) =>
  PopoutView.createSimpleDropdown({
    componentToShow: ResultAddView,
    modelForComponent: model,
    leftIcon: 'fa fa-plus',
    rightIcon: 'fa fa-chevron-down',
    label: 'Add / Remove from List',
  })

const AddToList = (props: any) => {
  const currentWorkspace = store.getCurrentWorkspace()
  if (!currentWorkspace) {
    return null
  }

  return (
    <MarionetteRegionContainer
      data-help="Add the result to a list."
      className="metacard-interaction interaction-add"
      view={createAddRemoveRegion(props.model)}
      viewOptions={{ model: props.model }}
    />
  )
}

export default hot(module)(AddToList)
