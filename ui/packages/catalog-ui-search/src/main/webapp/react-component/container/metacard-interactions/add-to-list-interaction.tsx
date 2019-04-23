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
