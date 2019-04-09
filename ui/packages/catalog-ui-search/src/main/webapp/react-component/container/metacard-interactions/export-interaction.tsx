import * as React from 'react'
const PopoutView = require('../../../component/dropdown/popout/dropdown.popout.view')
const ExportActionsView = require('../../../component/export-actions/export-actions.view')
import MarionetteRegionContainer from '../marionette-region-container'
import { Model, Props } from '.'
import { hot } from 'react-hot-loader'

const ExportActions = ({ model }: Props) => {
  return (
    <MarionetteRegionContainer
      data-help="Opens the available actions for the item."
      className="metacard-interaction interaction-actions-export composed-menu"
      view={createResultActionsExportRegion(model)}
      viewOptions={{ model }}
    />
  )
}

const createResultActionsExportRegion = (model: Model) =>
  PopoutView.createSimpleDropdown({
    componentToShow: ExportActionsView,
    dropdownCompanionBehaviors: {
      navigation: {},
    },
    modelForComponent: model.first(),
    leftIcon: 'fa fa-external-link',
    rightIcon: 'fa fa-chevron-down',
    label: 'Export as',
  })

export default hot(module)(ExportActions)
