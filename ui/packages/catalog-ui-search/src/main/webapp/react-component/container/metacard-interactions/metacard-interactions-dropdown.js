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
import MarionetteRegionContainer from '../marionette-region-container'
import MetacardInteractions from '.'
import { hot } from 'react-hot-loader'
import styled from 'styled-components'
const Marionette = require('marionette')
const CustomElements = require('../../../js/CustomElements.js')
require('../../../behaviors/dropdown.behavior')

const Button = styled.button`
  display: 'inline-block';
  text-align: center;
  vertical-align: top;
  width: ${props => props.theme.minimumButtonSize};
  height: ${props => props.theme.minimumButtonSize};
  line-height: ${props => props.theme.minimumButtonSize};
`

const ContainerView = Marionette.ItemView.extend({
  template() {
    return (
      <Button
        className="metacard-interactions is-button"
        title="Provides a list of actions to take on the result."
        data-help="Provides a list of actions to take on the result."
      >
        <span className="fa fa-ellipsis-v" />
      </Button>
    )
  },
  tagName: 'span',
  behaviors() {
    return {
      dropdown: {
        dropdowns: [
          {
            selector: '.metacard-interactions',
            view: MetacardInteractionsDropdown.extend({
              behaviors: {
                navigation: {},
              },
            }),
            viewOptions: {
              model: this.options.model,
            },
          },
        ],
      },
    }
  },

  initialize() {
    if (this.model.length === 1) {
      this.listenTo(
        this.model
          .first()
          .get('metacard')
          .get('properties'),
        'change',
        this.handleModelUpdates
      )
    }
  },
  handleModelUpdates() {
    this.render()
  },
})

const MetacardInteractionsDropdown = Marionette.ItemView.extend({
  template() {
    const props = {
      model: this.model,
      onClose: () => {
        this.$el.trigger(`closeDropdown.${CustomElements.getNamespace()}`)
      },
    }
    return <MetacardInteractions {...props} />
  },
  tagName: CustomElements.register('metacard-interactions-dropdown'),
  handleShare() {},
})

const Component = ({ model }) => (
  <MarionetteRegionContainer
    view={ContainerView}
    viewOptions={{ model }}
    replaceElement
  />
)

export default hot(module)(Component)
