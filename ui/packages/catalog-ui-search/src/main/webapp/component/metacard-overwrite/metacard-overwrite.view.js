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

const ConfirmationView = require('../confirmation/confirmation.view.js')
const Dropzone = require('dropzone')
const OverwritesInstance = require('../singletons/overwrites-instance.js')
import React from 'react'
import styled from 'styled-components'
import { readableColor } from 'polished'
import {
  Button,
  buttonTypeEnum,
} from '../../react-component/presentation/button'
import withListenTo from '../../react-component/backbone-container'

const Root = styled.div`
  overflow: auto;
  white-space: nowrap;
  height: 100%;
`

const OverwriteConfirm = styled(Button)`
  display: inline-block;
  white-space: normal;
  vertical-align: top !important;
  width: 100%;
  transform: translateX(0%);
  transition: transform ${props => props.theme.coreTransitionTime} linear;
  height: auto;
`

const MainText = styled.span`
  display: block;
  font-size: ${props => props.theme.largeFontSize};
`

const SubText = styled.span`
  display: block;
  font-size: ${props => props.theme.mediumFontSize};
`

const OverwriteStatus = styled.div`
  display: inline-block;
  white-space: normal;
  vertical-align: top !important;
  width: 100%;
  transform: translateX(0%);
  transition: transform ${props => props.theme.coreTransitionTime} linear;
  text-align: center;
  position: relative;
  white-space: normal;
  padding: 10px;
`

const OverwriteProgress = styled(OverwriteStatus)`
  line-height: ${props => props.theme.minimumButtonSize};
`

const ProgressText = styled.div`
  padding: 10px;
  top: 0px;
  left: 0px;
  position: absolute;
  width: 100%;
  height: 100%;
  z-index: 1;
  font-size: ${props => props.theme.largeFontSize};
  color: ${props => readableColor(props.theme.backgroundContent)};
`

const ProgressTextUnder = styled.div`
  font-size: ${props => props.theme.largeFontSize};
  visibility: hidden;
`

const ProgressInfo = styled.div`
  font-size: ${props => props.theme.mediumFontSize};
  color: ${props => readableColor(props.theme.backgroundContent)};
`

const ProgressBar = styled.div`
  z-index: 0;
  top: 0px;
  left: 0px;
  position: absolute;
  width: 0%;
  height: 100%;
  background: ${props => props.theme.positiveColor};
  transition: width ${props => props.theme.coreTransitionTime} linear;
`

const OverwriteSuccess = styled(OverwriteStatus)`
  color: ${props => readableColor(props.theme.positiveColor)};
  background: ${props => props.theme.positiveColor};
`

const OverwriteError = styled(OverwriteStatus)`
  color: ${props => readableColor(props.theme.negativeColor)};
  background: ${props => props.theme.negativeColor};
`

const ResultMessage = styled.div`
  font-size: ${props => props.theme.largeFontSize};
  margin-left: ${props => props.theme.minimumButtonSize};
`

const OverwriteBack = styled.button`
  position: absolute;
  left: 0px;
  top: 0px;
  width: ${props => props.theme.minimumButtonSize};
  height: 100%;
  text-align: center;
`

const Confirm = props => (
  <OverwriteConfirm
    buttonType={buttonTypeEnum.negative}
    onClick={props.archive}
    data-help="This will overwrite the item content. To restore a previous content, you can click on 'File' in the toolbar, and then click 'Restore Archived Items'."
  >
    <MainText>Overwrite content</MainText>
    <SubText>
      WARNING: This will completely overwrite the current content and metadata.
    </SubText>
  </OverwriteConfirm>
)

const Sending = props => (
  <OverwriteProgress>
    <ProgressTextUnder>
      Uploading File
      <div>{props.percentage}%</div>
      <ProgressInfo>
        If you leave this view, the overwrite will still continue.
      </ProgressInfo>
    </ProgressTextUnder>
    <ProgressText>
      Uploading File
      <div>{Math.floor(props.percentage)}%</div>
      <ProgressInfo>
        If you leave this view, the overwrite will still continue.
      </ProgressInfo>
    </ProgressText>
    <ProgressBar style={{ width: `${props.percentage}%` }} />
  </OverwriteProgress>
)

const Success = props => (
  <OverwriteSuccess>
    <OverwriteBack onClick={props.startOver}>
      <span className="fa fa-chevron-left" />
    </OverwriteBack>
    <ResultMessage>{props.message}</ResultMessage>
  </OverwriteSuccess>
)

const Error = props => (
  <OverwriteError>
    <OverwriteBack onClick={props.startOver}>
      <span className="fa fa-chevron-left" />
    </OverwriteBack>
    <ResultMessage>{props.message}</ResultMessage>
  </OverwriteError>
)

const Stages = {
  Confirm,
  Sending,
  Success,
  Error,
}

const defaultState = {
  stage: 'Confirm',
  percentage: 0,
  message: '',
}

const mapOverwriteModelToState = overwriteModel => {
  const currentState = {}
  if (overwriteModel.get('success')) {
    currentState.stage = 'Success'
  } else if (overwriteModel.get('error')) {
    currentState.stage = 'Error'
  } else if (overwriteModel.get('sending')) {
    currentState.stage = 'Sending'
  } else {
    currentState.stage = 'Confirm'
  }
  currentState.percentage = overwriteModel.get('percentage')
  currentState.message = overwriteModel.escape('message')

  return currentState
}

class MetacardOverwrite extends React.Component {
  constructor(props) {
    super(props)
    this.state = defaultState
    this.model = props.selectionInterface.getSelectedResults().first()
    this.dropzoneElement = React.createRef()
  }

  componentDidMount() {
    this.dropzone = new Dropzone(this.dropzoneElement.current, {
      url: './internal/catalog/' + this.model.get('metacard').id,
      maxFilesize: 5000000, //MB
      method: 'put',
    })
    this.trackOverwrite()
    this.setupEventListeners()
    this.setState(mapOverwriteModelToState(this.getOverwriteModel()))
  }

  render() {
    const Component = Stages[this.state.stage]
    return (
      <Root>
        <div style={{ display: 'none' }} ref={this.dropzoneElement} />
        <Component
          {...this.state}
          archive={() => this.archive()}
          startOver={() => this.startOver()}
        />
      </Root>
    )
  }

  getOverwriteModel() {
    return OverwritesInstance.get(this.model.get('metacard').id)
  }

  trackOverwrite() {
    if (!this.getOverwriteModel()) {
      OverwritesInstance.add({
        id: this.model.get('metacard').id,
        dropzone: this.dropzone,
        result: this.model,
      })
    }
  }

  setupEventListeners() {
    const overwriteModel = this.getOverwriteModel()
    this.props.listenTo(
      overwriteModel,
      'change:percentage change:sending change:error change:success',
      () => this.handleChange()
    )
  }

  handleChange() {
    const overwriteModel = this.getOverwriteModel()
    this.setState(mapOverwriteModelToState(overwriteModel))
  }

  archive() {
    this.props.listenTo(
      ConfirmationView.generateConfirmation({
        prompt: 'Are you sure you want to overwrite the content?',
        no: 'Cancel',
        yes: 'Overwrite',
      }),
      'change:choice',
      confirmation => {
        if (confirmation.get('choice')) {
          this.dropzoneElement.current.click()
        }
      }
    )
  }

  startOver() {
    OverwritesInstance.remove(this.model.get('metacard').id)
    this.trackOverwrite()
    this.setupEventListeners()
    this.setState(defaultState)
  }

  componentWillUnmount() {
    OverwritesInstance.removeIfUnused(this.model.get('metacard').id)
  }
}

export default withListenTo(MetacardOverwrite)
