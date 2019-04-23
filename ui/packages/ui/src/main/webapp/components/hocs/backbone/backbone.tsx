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
import { Subtract } from '../../../typescript'

const Backbone = require('backbone')

export type WithBackboneProps = {
  listenTo: (object: any, events: string, callback: Function) => any
  stopListening: (
    object?: any,
    events?: string | undefined,
    callback?: Function | undefined
  ) => any
  listenToOnce: (object: any, events: string, callback: Function) => any
}

const withListenTo = <P extends WithBackboneProps>(
  Component: React.ComponentType<P>
) => {
  return class BackboneContainer extends React.Component<
    Subtract<P, WithBackboneProps>,
    {}
  > {
    backbone: Backbone.Model = new Backbone.Model({})
    componentWillUnmount() {
      this.backbone.stopListening()
      this.backbone.destroy()
    }
    render() {
      return (
        <Component
          listenTo={this.backbone.listenTo.bind(this.backbone)}
          stopListening={this.backbone.stopListening.bind(this.backbone)}
          listenToOnce={this.backbone.listenToOnce.bind(this.backbone)}
          {...this.props}
        />
      )
    }
  }
}

export default withListenTo
