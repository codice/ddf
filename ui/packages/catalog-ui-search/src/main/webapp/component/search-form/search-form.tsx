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
import {
  Button,
  buttonTypeEnum,
} from '../../react-component/presentation/button'
import styled from '../../react-component/styles/styled-components'
import { hot } from 'react-hot-loader'

type Props = {
  type: string
  createdBy: string
  createdOn: string
  name: string
  changeView: () => void
}

const ModifiedButton = styled(Button)`
  height: 100%;
  width: 100%;
  padding: 0px;
  margin: 0px;
`

const Icon = styled.span`
  margin-bottom: 20px;
`

const render = ({ type, createdBy, createdOn, name, changeView }: Props) => {
  switch (type) {
    case 'new-form':
      return (
        <ModifiedButton
          buttonType={buttonTypeEnum.neutral}
          fadeUntilHover
          onClick={changeView}
        >
          <Icon className="fa fa-plus-circle new-form-circle" />
          <h3>New Search Form</h3>
        </ModifiedButton>
      )
      break
    case 'basic':
      return (
        <div>
          <h3>Basic</h3>
        </div>
      )
      break
    case 'text':
      return (
        <div>
          <h3>Text</h3>
        </div>
      )
      break
    case 'custom':
      return (
        <div>
          <h3 className="search-form-title" data-help={name}>
            {name}
          </h3>
          <div className="default-icon">
            <div className="fa fa-star" />
          </div>
          <span className="search-form-contents">{createdOn}</span>
          <span className="search-form-contents">
            <span className="fa fa-cloud" />
            {createdBy}
          </span>
          <span
            className="choice-actions is-button"
            title="Shows a list of actions to take on the search forms"
            data-help="Shows a list of actions to take on the search forms."
          />
        </div>
      )
      break
    case 'new-result':
      return (
        <div>
          <div className="fa fa-plus-circle new-form-circle" />
          <h3>New Result Form</h3>
        </div>
      )
      break
    case 'result':
      return (
        <div>
          <h3 className="search-form-title" data-help={name}>
            {name}
          </h3>
          <span className="search-form-contents">{createdOn}</span>
          <span className="search-form-contents">
            <span className="fa fa-cloud" />
            {createdBy}
          </span>
          <span
            className="choice-actions is-button"
            title="Shows a list of actions to take on the search forms"
            data-help="Shows a list of actions to take on the search forms."
          />
        </div>
      )
      break
    default:
      break
  }
}

export default hot(module)(render)
