import * as React from 'react'
const PopoutView = require('../../../component/dropdown/popout/dropdown.popout.view')
const ResultAddView = require('../../../component/result-add/result-add.view')
import { Model, Props } from '.'
const store = require('../../../js/store')
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

const AddToList = ({ model }: Props) => {
  const currentWorkspace = store.getCurrentWorkspace()
  if (!currentWorkspace) {
    return null
  }

  return (
    <MarionetteRegionContainer
      data-help="Add the result to a list."
      className="metacard-interaction interaction-add"
      view={createAddRemoveRegion(model)}
      viewOptions={{ model }}
    />
  )
}

export default hot(module)(AddToList)
