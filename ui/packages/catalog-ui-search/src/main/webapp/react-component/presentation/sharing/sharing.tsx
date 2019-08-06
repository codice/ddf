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

import { hot } from 'react-hot-loader'
import Text from '../../container/input-wrappers/text/text'
import Enum from '../../container/input-wrappers/enum/enum'
import { Category, Item } from '../../container/sharing'
import styled from '../../styles/styled-components/styled-components'
import { Access } from '../../utils/security'

const Root = styled.div`
  position: relative;
  width: 100%;
  height: 100%;
`

const Items = styled.div`
  top: 0;
  overflow: auto;
  position: absolute;
  width: 100%;
  bottom: 130px;
`

const Item = styled.div`
  margin: 0px 50px;
`

const IconAndName = styled.div`
  display: inline-block;
  width: 50%;
`

const ReadOnlyLabel = styled.span`
  margin-left: 12px;
`

const EditableLabelText = styled(Text)`
  display: inline-block;
  padding: 5px;
  width: 80%;
`

const AccessEnum = styled(Enum)`
  display: inline-block;
  width: calc(50% - 70px);
`

const Buttons = styled.div`
  position: absolute;
  bottom: 20px;
  left: 20px;
  width: calc(100% - 40px);
`

const DeleteButton = styled.button`
  display: inline-block;
  width: 50px;
  vertical-align: middle;
`

const FullWidthButton = styled.button`
  width: 100%;
  margin-bottom: 10px;
`

const HalfWidthButton = styled.button`
  width: 50%;
`

type Props = {
  items: Item[]
  add: () => void
  save: () => void
  reset: () => void
  remove: (i: number) => void
  handleChangeSelect: (i: number, value: Access) => void
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
    <Root>
      <Items>
        {items.map((item, i) => {
          return (
            item.visible && (
              <Item key={item.id}>
                <IconAndName>
                  <span
                    className={
                      item.category === Category.User
                        ? 'fa fa-user'
                        : 'fa fa-users'
                    }
                  />
                  {item.category === Category.User ? (
                    <EditableLabelText
                      value={item.value}
                      placeholder="user@example.com"
                      showLabel={false}
                      onChange={value => handleChangeInput(i, value)}
                    />
                  ) : (
                    <ReadOnlyLabel> {item.value} </ReadOnlyLabel>
                  )}
                </IconAndName>
                <AccessEnum
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
                  <DeleteButton
                    onClick={() => {
                      remove(i)
                    }}
                    className="is-negative"
                  >
                    <span className="fa fa-minus" />
                  </DeleteButton>
                )}
              </Item>
            )
          )
        })}
      </Items>
      <Buttons>
        <FullWidthButton
          onClick={() => {
            add()
          }}
          className="is-positive"
        >
          <span className="fa fa-plus" /> Add User
        </FullWidthButton>
        <HalfWidthButton
          className="is-negative reset"
          onClick={() => {
            reset()
          }}
        >
          <i className="fa fa-undo" aria-hidden="true" /> Reset
        </HalfWidthButton>
        <HalfWidthButton
          className="is-positive save"
          onClick={() => {
            save()
          }}
        >
          <i className="fa fa-floppy-o" aria-hidden="true" /> Apply
        </HalfWidthButton>
      </Buttons>
    </Root>
  )
}

export default hot(module)(render)
