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
import Enum from '../../react-component/container/input-wrappers/enum'
import Text from '../../react-component/container/input-wrappers/text'
import fetch from '../../react-component/utils/fetch'

const _ = require('underscore')
const user = require('component/singletons/user-instance')
const common = require('js/Common')
const announcement = require('component/announcement')

const deepCopy = function(o) {
  return JSON.parse(JSON.stringify(o))
}

const getGroups = function(groups = [], groupsRead = []) {
  // only display the roles the current user has (even if other roles have
  // permissions on this metacard)
  return user
    .get('user')
    .get('roles')
    .map(role => {
      return {
        id: common.generateUUID(),
        type: 'role',
        access: groups.includes(role)
          ? 'write'
          : groupsRead.includes(role)
            ? 'read'
            : 'none',
        value: role,
      }
    })
}

const getIndividuals = function(
  owner,
  individuals = [],
  individualsRead = [],
  accessAdministrators = []
) {
  return _.union(individuals, individualsRead, accessAdministrators)
    .filter(email => email !== owner) // hide owner
    .map(email => {
      return {
        id: common.generateUUID(),
        type: 'user',
        access: accessAdministrators.includes(email)
          ? 'share'
          : individuals.includes(email)
            ? 'write'
            : individualsRead.includes(email)
              ? 'read'
              : 'none',
        value: email,
      }
    })
}

export default class WorkspaceSharing extends React.Component {
  constructor(props) {
    super(props)

    this.state = {
      items: [],
    }
  }

  componentDidMount() {
    fetch(`/search/catalog/internal/metacard/${this.props.id}`)
      .then(response => response.json())
      .then(data => {
        const metacard = data.metacards[0]
        const items = getGroups(
          metacard['security.access-groups'],
          metacard['security.access-groups-read']
        ).concat(
          getIndividuals(
            metacard['metacard.owner'],
            metacard['security.access-individuals'],
            metacard['security.access-individuals-read'],
            metacard['security.access-administrators']
          )
        )
        this.setState({ items: items })
        this.add()
      })
  }

  save() {
    const roles = this.state.items.filter(e => e.type === 'role')
    const users = this.state.items.filter(
      e => e.value !== '' && e.type === 'user'
    )

    fetch(`/search/catalog/internal/metacards`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify([
        {
          ids: [this.props.id],
          attributes: [
            {
              attribute: 'security.access-individuals',
              values: users.filter(e => e.access === 'write').map(e => e.value),
            },
            {
              attribute: 'security.access-individuals-read',
              values: users.filter(e => e.access === 'read').map(e => e.value),
            },
            {
              attribute: 'security.access-groups',
              values: roles.filter(e => e.access === 'write').map(e => e.value),
            },
            {
              attribute: 'security.access-groups-read',
              values: roles.filter(e => e.access === 'read').map(e => e.value),
            },
            {
              attribute: 'security.access-administrators',
              values: users.filter(e => e.access === 'share').map(e => e.value),
            },
          ],
        },
      ]),
    })
      .then(res => res.json())
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

  add() {
    const updatedItems = deepCopy(this.state.items)
    updatedItems.push({
      id: common.generateUUID(),
      type: 'user',
      value: '',
      access: 'read',
    })
    this.setState({
      items: updatedItems,
    })
  }

  remove(i) {
    const updatedItems = deepCopy(this.state.items)
    updatedItems.splice(i, 1)
    this.setState({
      items: updatedItems,
    })
  }

  handleChangeInput(i, value) {
    const updatedItems = deepCopy(this.state.items)
    updatedItems[i].value = value
    this.setState({
      items: updatedItems,
    })
  }

  handleChangeSelect(i, value) {
    const updatedItems = deepCopy(this.state.items)
    updatedItems[i].access = value
    this.setState({
      items: updatedItems,
    })
  }

  reset() {
    // resetting to a saved initial state is the preferred style, but
    // the react wrappers do not currently support updating state properly
    this.componentDidMount()
  }

  render() {
    return (
      <div
        style={{
          position: 'relative',
          width: '100%',
          height: '100%',
        }}
      >
        <div
          style={{
            top: '0',
            overflow: 'auto',
            position: 'absolute',
            width: '100%',
            bottom: '130px',
          }}
        >
          {this.state.items.map((item, i) => {
            return (
              <div key={item.id} style={{ margin: '0 50px' }}>
                <div style={{ display: 'inline-block', width: '50%' }}>
                  <span
                    className={
                      item.type === 'user' ? 'fa fa-user' : 'fa fa-users'
                    }
                  />
                  {item.type === 'user' ? (
                    <Text
                      style={{
                        display: 'inline-block',
                        padding: '5px',
                        width: '80%',
                      }}
                      value={item.value}
                      placeholder="Enter a user's email"
                      showLabel={false}
                      onChange={value => this.handleChangeInput(i, value)}
                    />
                  ) : (
                    <span style={{ marginLeft: '12px' }}> {item.value} </span>
                  )}
                </div>
                <Enum
                  style={{ display: 'inline-block', width: 'calc(50% - 70px)' }}
                  options={
                    item.type === 'user'
                      ? [
                          { label: 'No Access', value: 'none' },
                          { label: 'Read Only', value: 'read' },
                          { label: 'Read and Write', value: 'write' },
                          { label: 'Read, Write, and Share', value: 'share' },
                        ]
                      : [
                          { label: 'No Access', value: 'none' },
                          { label: 'Read Only', value: 'read' },
                          { label: 'Read and Write', value: 'write' },
                        ]
                  }
                  value={item.access}
                  showLabel={false}
                  onChange={value => this.handleChangeSelect(i, value)}
                />
                {item.type === 'user' && (
                  <button
                    style={{
                      display: 'inline-block',
                      width: '50px',
                      verticalAlign: 'middle',
                    }}
                    onClick={() => {
                      this.remove(i)
                    }}
                    className="is-negative"
                  >
                    <span className="fa fa-minus" />
                  </button>
                )}
              </div>
            )
          })}
        </div>
        <div
          style={{
            position: 'absolute',
            bottom: '20px',
            left: '20px',
            width: 'calc(100% - 40px)',
          }}
        >
          <button
            style={{ width: '100%', marginBottom: '10px' }}
            onClick={() => {
              this.add()
            }}
            className="is-positive"
          >
            <span className="fa fa-plus" /> Add User
          </button>
          <button
            className="is-negative reset"
            style={{ width: '50%' }}
            onClick={() => {
              this.reset()
            }}
          >
            <i className="fa fa-undo" aria-hidden="true" /> Reset
          </button>
          <button
            className="is-positive save"
            style={{ width: '50%' }}
            onClick={() => {
              this.save()
            }}
          >
            <i className="fa fa-floppy-o" aria-hidden="true" /> Apply
          </button>
        </div>
      </div>
    )
  }
}
