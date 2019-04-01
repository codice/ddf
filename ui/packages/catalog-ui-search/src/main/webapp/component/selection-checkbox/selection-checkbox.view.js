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
const Marionette = require('marionette')
import React from 'react'
import styled from '../../react-component/styles/styled-components'

const Root = styled.div`
  display: block;
  width: ${props => props.theme.minimumSpacing};
  padding-right: ${props => props.theme.minimumButtonSize};
  cursor: pointer;
`

module.exports = Marionette.ItemView.extend({
  events: {
    'click span': 'handleClick',
  },
  handleClick(e) {
    e.stopPropagation()
    this.setCheck(!this.isSelected)
    this.options.onClick(this.isSelected)
  },
  template() {
    return (
      <Root>
        {this.isSelected ? (
          <span className="fa fa-check-square-o" />
        ) : (
          <span className="fa fa-square-o" />
        )}
      </Root>
    )
  },
  initialize() {
    this.isSelected = this.options.isSelected
  },
  setCheck: function(isSelected) {
    this.isSelected = isSelected
    this.render()
  },
})
