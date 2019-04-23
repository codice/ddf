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
import SharingPresentation from '../../presentation/sharing'
import fetch from '../../utils/fetch/index'
import { Restrictions, Access, Security, Entry } from '../../utils/security'

const user = require('component/singletons/user-instance')
const common = require('js/Common')
const announcement = require('component/announcement')
const LoadingView = require('../../../component/loading/loading.view')

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
  isWorkspace: boolean
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
  prevUsers: any
  constructor(props: Props) {
    super(props)
    this.state = {
      items: [],
      previousWorkspace: undefined,
      isWorkspace: false,
    }
  }
  componentDidMount = () => {
    this.fetchWorkspace(this.props.id).then(data => {
      const metacard = data
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
        isWorkspace: data['metacard-tags'].includes('workspace'),
      })
      this.add()
    })
  }

  save = () => {
    let usersToUnsubscribe: string[] = []
    const groups = this.state.items.filter(e => e.category === Category.Group)
    const guest = this.state.items.filter(
      e => e.category === Category.Group && e.value === 'guest'
    )
    const users = this.state.items.filter(
      e => e.value !== '' && e.category === Category.User
    )

    if (this.state.isWorkspace && guest[0].access === 0) {
      usersToUnsubscribe = this.getUsersToUnsubscribe(users)
    }
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

    const loadingView = new LoadingView()
    this.attemptSave(attributes, usersToUnsubscribe)
      .then(() => {
        this.showSaveSuccessful()
        this.props.lightbox.close()
      })
      .catch(err => {
        if (err.message === 'Need to refresh') {
          this.showNeedToRefresh()
        } else {
          this.showSaveFailed()
        }
      })
      .then(() => {
        loadingView.remove()
      })
  }

  // NOTE: Fetching the latest workspace and checking the modified dates is a temporary solution
  // and should be removed when support for optimistic concurrency is added
  // https://github.com/codice/ddf/issues/4467
  attemptSave = async (attributes: any, usersToUnsubscribe: String[]) => {
    const currWorkspace = await this.fetchWorkspace(this.props.id)
    if (
      currWorkspace['metacard.modified'] ===
      this.state.previousWorkspace['metacard.modified']
    ) {
      await this.doSave(attributes)
      await this.unsubscribeUsers(usersToUnsubscribe)
      const newWorkspace = await this.fetchWorkspace(this.props.id)
      this.setState({
        items: [...this.state.items],
        previousWorkspace: newWorkspace,
      })
    } else {
      throw new Error('Need to refresh')
    }
  }

  doSave = async (attributes: any) => {
    const res = await fetch(`/search/catalog/internal/metacards`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify([
        {
          ids: [this.props.id],
          attributes: attributes,
        },
      ]),
    })

    if (res.status !== 200) {
      throw new Error()
    }

    if (this.props.onUpdate) {
      this.props.onUpdate(attributes)
    }
    return await res.json()
  }

  fetchWorkspace = async (id: number) => {
    const res = await fetch('/search/catalog/internal/metacard/' + id)
    const workspace = await res.json()
    return workspace.metacards[0]
  }

  unsubscribeUsers = async (usersToUnsubscribe: String[]) => {
    if (usersToUnsubscribe.length === 0) {
      return
    }
    const res = await fetch(
      '/search/catalog/internal/unsubscribe/' + this.props.id,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          attribute: 'unsubscribedUsers',
          values: usersToUnsubscribe,
        }),
      }
    )
    return await res.json()
  }

  showSaveFailed() {
    announcement.announce(
      {
        title: 'Error',
        message: 'Save failed',
        type: 'error',
      },
      1500
    )
  }

  showNeedToRefresh() {
    announcement.announce(
      {
        title: 'The workspace settings could not be updated',
        message:
          'The workspace has been modified by another user. Please refresh the page and reattempt your changes.',
        type: 'error',
      },
      1500
    )
  }

  showSaveSuccessful() {
    announcement.announce(
      {
        title: 'Success!',
        message: 'Sharing saved',
        type: 'success',
      },
      1500
    )
  }

  getUsersToUnsubscribe(users: Item[]) {
    let usersToUnsubscribe: string[] = []
    const usersWithReadOrHigher = users.filter(e => e.access !== 0)
    if (this.prevUsers === undefined) {
      this.prevUsers = usersWithReadOrHigher
    } else if (this.prevUsers !== usersWithReadOrHigher) {
      this.prevUsers.forEach((user: Item) => {
        if (!usersWithReadOrHigher.includes(user)) {
          usersToUnsubscribe.push(user.value)
        }
      })
      this.prevUsers = usersWithReadOrHigher
    }
    return usersToUnsubscribe
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
