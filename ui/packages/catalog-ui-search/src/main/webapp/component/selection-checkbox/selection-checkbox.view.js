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
import styled from 'styled-components'

const Root = styled.div`
  display: inline-block;
  width: auto;
  cursor: pointer;
`

const createToggle = ({ isSelected, handleClick }) => {
  return Marionette.ItemView.extend({
    events: {
      'click span': 'handleClick',
    },
    handleClick(e) {
      e.stopPropagation()
      handleClick.call(this)
    },
    template() {
      const className = `fa fa-${this.isSelected() ? 'check-' : ''}square-o`
      return (
        <Root>
          <span className={className} />
        </Root>
      )
    },
    isSelected() {
      return isSelected.call(this)
    },
    initialize() {
      this.listenTo(
        this.options.selectionInterface.getSelectedResults(),
        'update add remove reset',
        this.render
      )
      this.listenTo(
        this.options.selectionInterface.getActiveSearchResults(),
        'update add remove reset',
        this.render
      )
    },
  })
}

const SelectItemToggle = createToggle({
  handleClick() {
    if (this.isSelected()) {
      this.options.selectionInterface.removeSelectedResult(this.model)
    } else {
      this.options.selectionInterface.addSelectedResult(this.model)
    }
  },
  isSelected() {
    const selectedResults = this.options.selectionInterface.getSelectedResults()
    return Boolean(selectedResults.get(this.model.id))
  },
})

const SelectAllToggle = createToggle({
  handleClick() {
    if (this.isSelected()) {
      this.options.selectionInterface.clearSelectedResults()
    } else {
      const currentResults = this.options.selectionInterface.getActiveSearchResults()
      this.options.selectionInterface.setSelectedResults(currentResults.models)
    }
  },
  isSelected() {
    const currentResults = this.options.selectionInterface.getActiveSearchResults()
    const selectedResults = this.options.selectionInterface.getSelectedResults()
    return (
      currentResults.length > 0 &&
      selectedResults.length >= currentResults.length &&
      currentResults.every(
        currentResult => selectedResults.get(currentResult) !== undefined
      )
    )
  },
})

export { SelectItemToggle, SelectAllToggle }
