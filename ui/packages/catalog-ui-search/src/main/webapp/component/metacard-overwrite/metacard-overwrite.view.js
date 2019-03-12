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
/*global require*/
const store = require('../../js/store.js')
const ConfirmationView = require('../confirmation/confirmation.view.js')
const Dropzone = require('dropzone')
const OverwritesInstance = require('../singletons/overwrites-instance.js')
import React from 'react'
import ReactDOM from 'react-dom'
import styled from '../../react-component/styles/styled-components'
import { readableColor } from 'polished'
import {
  Button,
  buttonTypeEnum,
} from '../../react-component/presentation/button'
import withListenTo from '../../react-component/container/backbone-container'

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
    onClick={props.onClick}
    data-help="This will overwrite the item content. To restore a previous content, you can click on 'File' in the toolbar, and then click 'Restore Archived Items'."
  >
    <MainText>Overwrite content</MainText>
    <SubText>
      WARNING: This will completely overwrite the current content and metadata.
    </SubText>
  </OverwriteConfirm>
)

const Progress = props => (
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
    <OverwriteBack onClick={props.onClick}>
      <span className="fa fa-chevron-left" />
    </OverwriteBack>
    <ResultMessage>{props.message}</ResultMessage>
  </OverwriteSuccess>
)

const Error = props => (
  <OverwriteError>
    <OverwriteBack onClick={props.onClick}>
      <span className="fa fa-chevron-left" />
    </OverwriteBack>
    <ResultMessage>{props.message}</ResultMessage>
  </OverwriteError>
)

const defaultState = {
  sending: false,
  success: false,
  error: false,
  percentage: 0,
  message: '',
}

class MetacardOverwrite extends React.Component {
  constructor(props) {
    super(props)
    this.selectionInterface = props.selectionInterface || store
    this.state = defaultState
    this.state.model =
      props.model || this.selectionInterface.getSelectedResults().first()
    this.dropzoneElement = React.createRef()
  }

  componentDidMount() {
    this.dropzone = new Dropzone(
      ReactDOM.findDOMNode(this.dropzoneElement.current),
      {
        url: './internal/catalog/' + this.state.model.get('metacard').id,
        maxFilesize: 5000000, //MB
        method: 'put',
      }
    )
    this.trackOverwrite()
    this.setupEventListeners()
    this.handleSending()
    this.handlePercentage()
    this.handleError()
    this.handleSuccess()
  }

  render() {
    let Component
    if (this.state.success) {
      Component = () => (
        <Success
          onClick={() => this.startOver()}
          message={this.state.message}
        />
      )
    } else if (this.state.error) {
      Component = () => (
        <Error onClick={() => this.startOver()} message={this.state.message} />
      )
    } else if (this.state.sending) {
      Component = () => <Progress percentage={this.state.percentage} />
    } else {
      Component = () => <Confirm onClick={() => this.archive()} />
    }

    return (
      <Root>
        <div style={{ display: 'none' }} ref={this.dropzoneElement} />
        <Component />
      </Root>
    )
  }

  getOverwriteModel() {
    return OverwritesInstance.get(this.state.model.get('metacard').id)
  }

  trackOverwrite() {
    if (!this.getOverwriteModel()) {
      OverwritesInstance.add({
        id: this.state.model.get('metacard').id,
        dropzone: this.dropzone,
        result: this.state.model,
      })
    }
  }

  setupEventListeners() {
    const overwriteModel = this.getOverwriteModel()
    this.props.listenTo(overwriteModel, 'change:percentage', () =>
      this.handlePercentage()
    )
    this.props.listenTo(overwriteModel, 'change:sending', () =>
      this.handleSending()
    )
    this.props.listenTo(overwriteModel, 'change:error', () =>
      this.handleError()
    )
    this.props.listenTo(overwriteModel, 'change:success', () =>
      this.handleSuccess()
    )
  }

  handleSending() {
    const sending = this.getOverwriteModel().get('sending')
    this.setState({ sending })
  }

  handlePercentage() {
    const percentage = this.getOverwriteModel().get('percentage')
    this.setState({ percentage })
  }

  handleError() {
    const error = this.getOverwriteModel().get('error')
    const message = this.getOverwriteModel().escape('message')
    this.setState({ error, message })
  }

  handleSuccess() {
    const success = this.getOverwriteModel().get('success')
    const message = this.getOverwriteModel().escape('message')
    this.setState({ success, message })
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
    OverwritesInstance.remove(this.state.model.get('metacard').id)
    this.trackOverwrite()
    this.setupEventListeners()
    this.setState(defaultState)
  }

  componentWillUnmount() {
    OverwritesInstance.removeIfUnused(this.state.model.get('metacard').id)
  }
}

export default withListenTo(MetacardOverwrite)
