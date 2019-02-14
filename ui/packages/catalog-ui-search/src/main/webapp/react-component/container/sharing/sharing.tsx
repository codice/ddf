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
import SharingPresentation from '../../presentation/sharing'
import fetch from '../../utils/fetch/index'
import { Restrictions, Access, Security, Entry } from '../../utils/security'

const user = require('component/singletons/user-instance')
const common = require('js/Common')
const announcement = require('component/announcement')

type Attribute = {
  attribute: string
  values: string[]
}

type Props = {
  id: number
  lightbox: any
  onUpdate?: (attributes: Attribute[]) => void
}

type State = {
  items: Item[]
  previousWorkspace: any
}

export enum Category {
  User = 'user',
  Group = 'group',
}

export type Item = {
  id: string
  value: string
  visible: boolean
  category: Category
  access: Access
}

export class Sharing extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)

    this.state = {
      items: [],
      previousWorkspace: undefined,
    }
  }
  componentDidMount = () => {
    fetch(`/search/catalog/internal/metacard/${this.props.id}`)
      .then(response => response.json())
      .then(data => {
        const metacard = data.metacards[0]
        const res = Restrictions.from(metacard)
        const security = new Security(res)
        const individuals = security.getIndividuals().map((e: Entry) => {
          return {
            ...e,
            id: common.generateUUID(),
            category: Category.User,
            visible: e.value !== res.owner, // hide owner
          } as Item
        })
        const groups = security.getGroups(user.getRoles()).map((e: Entry) => {
          return {
            ...e,
            id: common.generateUUID(),
            category: Category.Group,
            visible: user.getRoles().indexOf(e.value) > -1, // only display the groups the current user has
          } as Item
        })
        this.setState({
          items: groups.concat(individuals),
          previousWorkspace: metacard,
        })
        this.add()
      })
  }

  save = () => {
    debugger
    const groups = this.state.items.filter(e => e.category === Category.Group)
    const users = this.state.items.filter(
      e => e.value !== '' && e.category === Category.User
    )

    const attributes = [
      {
        attribute: Restrictions.IndividualsWrite,
        values: users.filter(e => e.access === Access.Write).map(e => e.value),
      },
      {
        attribute: Restrictions.IndividualsRead,
        values: users.filter(e => e.access === Access.Read).map(e => e.value),
      },
      {
        attribute: Restrictions.GroupsWrite,
        values: groups.filter(e => e.access === Access.Write).map(e => e.value),
      },
      {
        attribute: Restrictions.GroupsRead,
        values: groups.filter(e => e.access === Access.Read).map(e => e.value),
      },
      {
        attribute: Restrictions.AccessAdministrators,
        values: users.filter(e => e.access === Access.Share).map(e => e.value),
      },
    ]
    this.attemptSave(attributes)
  }

  attemptSave = (attributes: any) => {
    fetch('/search/catalog/internal/metacard/' + this.props.id)
      .then(res => res.json())
      .then(data => {
        // Only allow a user to save if the previous workspace state is the same as the latest workspace state
        // so that changes made by other users are not clobbered.
        // NOTE: This is a temporary workaround.
        if (
          JSON.stringify(data.metacards[0]) ===
          JSON.stringify(this.state.previousWorkspace)
        ) {
          this.doSave(attributes)
        } else {
          announcement.announce(
            {
              title: 'The workspace settings could not be updated',
              message:
                'The workspace has been modified by another. Please refresh the page and reattempt your changes.',
              type: 'error',
            },
            1500
          )
        }
      })
      .catch(function() {
        announcement.announce(
          {
            title: 'Error',
            message: 'Save failed',
            type: 'error',
          },
          1500
        )
      })
  }

  doSave = (attributes: any) => {
    fetch(`/search/catalog/internal/metacards`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify([
        {
          ids: [this.props.id],
          attributes: attributes,
        },
      ]),
    })
      .then(res => {
        if (res.status !== 200) {
          throw new Error()
        }
        return res.json()
      })
      .then(() => {
        if (this.props.onUpdate) {
          this.props.onUpdate(attributes)
        }

        this.props.lightbox.close()
        announcement.announce(
          {
            title: 'Success!',
            message: 'Sharing saved',
            type: 'success',
          },
          1500
        )
      })
      .catch(function() {
        announcement.announce(
          {
            title: 'Error',
            message: 'Save failed',
            type: 'error',
          },
          1500
        )
      })
  }

  add = () => {
    this.state.items.push({
      id: common.generateUUID(),
      value: '',
      visible: true,
      category: Category.User,
      access: Access.Read,
    })
    this.setState({
      items: this.state.items,
    })
  }

  remove = (i: number) => {
    this.state.items.splice(i, 1)
    this.setState({
      items: this.state.items,
    })
  }

  handleChangeInput = (i: number, value: string) => {
    this.state.items[i].value = value
    this.setState({
      items: this.state.items,
    })
  }

  handleChangeSelect = (i: number, value: Access) => {
    this.state.items[i].access = value
    this.setState({
      items: this.state.items,
    })
  }

  reset = () => {
    // resetting to a saved initial state is the preferred style, but
    // the react wrappers do not currently support updating state properly
    this.componentDidMount()
  }

  render() {
    return (
      <SharingPresentation
        items={this.state.items}
        add={this.add}
        save={this.save}
        reset={this.reset}
        remove={this.remove}
        handleChangeSelect={this.handleChangeSelect}
        handleChangeInput={this.handleChangeInput}
      />
    )
  }
}
