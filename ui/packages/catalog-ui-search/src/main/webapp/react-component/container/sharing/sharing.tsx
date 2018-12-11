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

const _ = require('underscore')
const user = require('component/singletons/user-instance')
const common = require('js/Common')
const announcement = require('component/announcement')

type Props = {
  id: number
  lightbox: any
}

type State = {
  items: Item[]
}

export enum Category {
  User = 'user',
  Role = 'role',
}

export type Item = {
  id: string
  value: string
  editable: boolean
  category: Category
  access: Access
}

export enum Access {
  None = 'none',
  Read = 'read',
  Write = 'write',
  Share = 'share',
}

export enum Security {
  GroupsRead = 'security.access-groups-read',
  GroupsWrite = 'security.access-groups',
  IndividualsRead = 'security.access-individuals-read',
  IndividualsWrite = 'security.access-individuals',
  AccessAdministrators = 'security.access-administrators',
}

const getGroups = function(
  groupsWrite: string[],
  groupsRead: string[]
): Item[] {
  // only display the roles the current user has (even if other roles have
  // permissions on this metacard)
  return user
    .get('user')
    .get('roles')
    .map((role: string) => {
      return {
        id: common.generateUUID(),
        category: Category.Role,
        editable: true,
        access:
          groupsWrite.indexOf(role) > -1
            ? Access.Write
            : groupsRead.indexOf(role) > -1
              ? Access.Read
              : Access.None,
        value: role,
      } as Item
    })
}

const getIndividuals = function(
  owner: string,
  individualsWrite: string[],
  individualsRead: string[],
  accessAdministrators: string[]
): Item[] {
  return _.union(individualsWrite, individualsRead, accessAdministrators).map(
    (id: string) => {
      return {
        id: common.generateUUID(),
        category: Category.User,
        editable: id !== owner,
        access:
          accessAdministrators.indexOf(id) > -1
            ? Access.Share
            : individualsWrite.indexOf(id) > -1
              ? Access.Write
              : individualsRead.indexOf(id) > -1
                ? Access.Read
                : Access.None,
        value: id,
      } as Item
    }
  )
}

export class Sharing extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)

    this.state = {
      items: [],
    }
  }

  private static _compareFn = (a: Item, b: Item): number => {
    return a.value < b.value ? -1 : a.value > b.value ? 1 : 0
  }

  componentDidMount = () => {
    fetch(`/search/catalog/internal/metacard/${this.props.id}`)
      .then(response => response.json())
      .then(data => {
        const metacard = data.metacards[0]
        const items = getGroups(
          metacard[Security.GroupsWrite] || [],
          metacard[Security.GroupsRead] || []
        )
          .sort(Sharing._compareFn)
          .concat(
            getIndividuals(
              metacard['metacard.owner'] || [],
              metacard[Security.IndividualsWrite] || [],
              metacard[Security.IndividualsRead] || [],
              metacard[Security.AccessAdministrators] || []
            ).sort(Sharing._compareFn)
          )
        this.setState({ items: items })
        this.add()
      })
  }

  save = () => {
    const roles = this.state.items.filter(e => e.category === Category.Role)
    const users = this.state.items.filter(
      e => e.value !== '' && e.category === Category.User
    )

    fetch(`/search/catalog/internal/metacards`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify([
        {
          ids: [this.props.id],
          attributes: [
            {
              attribute: Security.IndividualsWrite,
              values: users
                .filter(e => e.access === Access.Write)
                .map(e => e.value),
            },
            {
              attribute: Security.IndividualsRead,
              values: users
                .filter(e => e.access === Access.Read)
                .map(e => e.value),
            },
            {
              attribute: Security.GroupsWrite,
              values: roles
                .filter(e => e.access === Access.Write)
                .map(e => e.value),
            },
            {
              attribute: Security.GroupsRead,
              values: roles
                .filter(e => e.access === Access.Read)
                .map(e => e.value),
            },
            {
              attribute: Security.AccessAdministrators,
              values: users
                .filter(e => e.access === Access.Share)
                .map(e => e.value),
            },
          ],
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
      editable: true,
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
