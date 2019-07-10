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
import { Props as PresentationProps } from './search-interactions.presentation'
import { hot } from 'react-hot-loader'
import withListenTo, {
  WithBackboneProps,
} from '../../react-component/container/backbone-container'
const user = require('../../component/singletons/user-instance.js')
const ConfirmationView = require('../../component/confirmation/confirmation.view.js')

type Props = {
  model: any
  onClose: () => void
  children: (props: PresentationProps) => React.ReactNode
} & WithBackboneProps

class SearchInteractions extends React.Component<Props> {
  componentDidMount() {
    this.props.listenTo(this.props.model, 'change:type', this.props.onClose)
  }
  triggerQueryForm = (formId: any) => {
    this.props.model.set('type', formId)
    user.getQuerySettings().set('type', formId)
    user.savePreferences()
    this.props.onClose()
  }
  triggerReset = () => {
    this.props.listenTo(
      ConfirmationView.generateConfirmation({
        prompt: 'Are you sure you want to reset the search?',
        no: 'Cancel',
        yes: 'Reset',
      }),
      'change:choice',
      (confirmation: any) => {
        if (confirmation.get('choice')) {
          const defaults =
            this.props.model.get('type') === 'custom'
              ? this.props.model.toJSON()
              : undefined
          this.props.model.resetToDefaults(defaults)
          this.props.onClose()
        }
      }
    )
  }
  render() {
    const { children, model } = this.props
    return children({
      model: model,
      triggerReset: this.triggerReset,
      triggerQueryForm: this.triggerQueryForm,
    })
  }
}

export default hot(module)(withListenTo(SearchInteractions))
