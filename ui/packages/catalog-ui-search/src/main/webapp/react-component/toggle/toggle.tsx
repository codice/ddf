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

type Props = {
  value: any
  cid: any
  placeholder: any
  id: any
}

const render = ({ cid, placeholder, id, value }: Props) => {
  return (
    <>
      <div className="if-editing">
        <label>
          <input id={cid} placeholder={placeholder} name={id} type="checkbox" />
          <button className="checkbox-placeholder is-button">
            <span className="is-not-checked fa fa-toggle-off" />
            <span className="is-checked fa fa-toggle-on" />
          </button>
        </label>
      </div>
      <div className="if-viewing">
        <label>{value}</label>
      </div>
    </>
  )
}

export default hot(module)(render)
