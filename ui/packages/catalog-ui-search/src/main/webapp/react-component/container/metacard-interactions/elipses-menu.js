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
/*global define*/
import * as React from 'react'
import MarionetteRegionContainer from '../marionette-region-container/index.jsx'
import { hot } from 'react-hot-loader'
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const MetacardInteractionsView = require('../metacard-interactions/metacard-interactions.view.js')
require('../../behaviors/dropdown.behavior.js')

const View = Marionette.ItemView.extend({
  template() {
    return (
      <button
        className="metacard-interactions is-button"
        title="Provides a list of actions to take on the result."
        data-help="Provides a list
                        of actions to take on the result."
      >
        <span className="fa fa-ellipsis-v" />
      </button>
    )
  },
  tagName: CustomElements.register('metacard-title'),
  behaviors() {
    return {
      dropdown: {
        dropdowns: [
          {
            selector: '.metacard-interactions',
            view: MetacardInteractionsView.extend({
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
  initialize: function() {
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
  handleModelUpdates: function() {
    this.render()
    this.onBeforeShow()
  },
})

const Component = ({ model }) => (
  <MarionetteRegionContainer view={View} viewOptions={{ model }} />
)

export default hot(module)(Component)
