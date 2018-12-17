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

import { hot } from 'react-hot-loader'
import Text from '../../container/input-wrappers/text/text'
import Enum from '../../container/input-wrappers/enum/enum'
import { Access, Category, Item } from '../../container/sharing'

type Props = {
  items: Item[]
  add: () => void
  save: () => void
  reset: () => void
  remove: (i: number) => void
  handleChangeSelect: (i: number, value: string) => void
  handleChangeInput: (i: number, value: string) => void
}

const render = (props: Props) => {
  const {
    items,
    add,
    save,
    reset,
    remove,
    handleChangeSelect,
    handleChangeInput,
  } = props
  const roleDropdown = [
    { label: 'No Access', value: Access.None },
    { label: 'Read Only', value: Access.Read },
    { label: 'Read and Write', value: Access.Write },
  ]
  const userDropdown = roleDropdown.slice()
  userDropdown.push({ label: 'Read, Write, and Share', value: Access.Share })
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
        {items.map((item, i) => {
          return (
            item.visible && (
              <div key={item.id} style={{ margin: '0 50px' }}>
                <div style={{ display: 'inline-block', width: '50%' }}>
                  <span
                    className={
                      item.category === Category.User
                        ? 'fa fa-user'
                        : 'fa fa-users'
                    }
                  />
                  {item.category === Category.User ? (
                    <Text
                      style={{
                        display: 'inline-block',
                        padding: '5px',
                        width: '80%',
                      }}
                      value={item.value}
                      placeholder="user@example.com"
                      showLabel={false}
                      onChange={value => handleChangeInput(i, value)}
                    />
                  ) : (
                    <span style={{ marginLeft: '12px' }}> {item.value} </span>
                  )}
                </div>
                <Enum
                  style={{
                    display: 'inline-block',
                    width: 'calc(50% - 70px)',
                  }}
                  options={
                    item.category === Category.User
                      ? userDropdown
                      : roleDropdown
                  }
                  value={item.access}
                  showLabel={false}
                  onChange={value => handleChangeSelect(i, value)}
                />
                {item.category === Category.User && (
                  <button
                    style={{
                      display: 'inline-block',
                      width: '50px',
                      verticalAlign: 'middle',
                    }}
                    onClick={() => {
                      remove(i)
                    }}
                    className="is-negative"
                  >
                    <span className="fa fa-minus" />
                  </button>
                )}
              </div>
            )
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
            add()
          }}
          className="is-positive"
        >
          <span className="fa fa-plus" /> Add User
        </button>
        <button
          className="is-negative reset"
          style={{ width: '50%' }}
          onClick={() => {
            reset()
          }}
        >
          <i className="fa fa-undo" aria-hidden="true" /> Reset
        </button>
        <button
          className="is-positive save"
          style={{ width: '50%' }}
          onClick={() => {
            save()
          }}
        >
          <i className="fa fa-floppy-o" aria-hidden="true" /> Apply
        </button>
      </div>
    </div>
  )
}

export default hot(module)(render)
