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
/**
 * Copyright (c) Connexta, LLC
 *
 * <p>Unlimited Government Rights (FAR Subpart 27.4) Government right to use, disclose, reproduce,
 * prepare derivative works, distribute copies to the public, and perform and display publicly, in
 * any manner and for any purpose, and to have or permit others to do so.
 */
import * as React from 'react'
import styled from '../../styles/styled-components'
import withListenTo from '../../container/backbone-container'
import MarionetteRegionContainer from '../../container/marionette-region-container'
import { buttonTypeEnum, Button } from '../button'

const ListEditorView = require('../../../component/list-editor/list-editor.view.js')
const List = require('../../../js/model/List.js')
const store = require('../../../js/store.js')
const ConfirmationView = require('../../../component/confirmation/confirmation.view.js')

const CreateContainer = styled.div`
  padding: ${props => props.theme.minimumSpacing};
`

const AddButton = props => {
  const { children, onClick } = props
  return (
    <Button
      buttonType={buttonTypeEnum.primary}
      onClick={onClick}
      style={{ width: '100%' }}
    >
      <span className="fa fa-plus" />
      <span>{children}</span>
    </Button>
  )
}

const createListView = () => {
  return new ListEditorView({
    model: new List(),
    showListTemplates: true,
    showFooter: false,
  })
}

class ListCreate extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      listEditor: createListView(),
    }
  }

  createList = () => {
    this.state.listEditor.save()
    store
      .getCurrentWorkspace()
      .get('lists')
      .add(this.state.listEditor.model)
    this.setState({ listEditor: createListView() })
  }

  createListWithBookmarks = () => {
    this.state.listEditor.save()
    if (
      this.props.model.every(result => {
        return result.matchesCql(this.state.listEditor.model.get('list.cql'))
      })
    ) {
      this.state.listEditor.model.addBookmarks(
        this.props.model.map(result => {
          return result.get('metacard').id
        })
      )
      setTimeout(() => {
        store
          .getCurrentWorkspace()
          .get('lists')
          .add(this.state.listEditor.model, { preventSwitch: true })
        this.setState({ listEditor: createListView() })
      }, 50)
    } else {
      this.props.listenTo(
        ConfirmationView.generateConfirmation({
          prompt:
            "This list's filter prevents the result from being in the list.  Create list without result?",
          no: 'Cancel',
          yes: 'Create',
        }),
        'change:choice',
        confirmation => {
          if (confirmation.get('choice')) {
            store
              .getCurrentWorkspace()
              .get('lists')
              .add(this.state.listEditor.model, { preventSwitch: true })
            this.setState({ listEditor: createListView() })
          }
        }
      )
    }
  }

  render() {
    return (
      <CreateContainer>
        <MarionetteRegionContainer view={this.state.listEditor} />
        {this.props.withBookmarks ? (
          <AddButton onClick={this.createListWithBookmarks}>
            Create New List with Result(s)
          </AddButton>
        ) : (
          <AddButton onClick={this.createList}>Create New List</AddButton>
        )}
      </CreateContainer>
    )
  }
}

export default withListenTo(ListCreate)
