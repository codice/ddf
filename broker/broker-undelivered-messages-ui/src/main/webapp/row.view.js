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
import React from "react"
import moment from 'moment'
import CheckBox from './checkbox.view'
import dispatch, {expandMessage, checkMessage} from './actions'

export default ({checked, id, timestamp, message, expanded}) => {
    return (
        <tr
            className={checked ? "highlighted" : "not-highlighted"}
            onClick={() => dispatch(checkMessage(id))}>
            <td><CheckBox checked={checked}/></td>
            <td>{moment(timestamp).format("hh:mm:ss, DD MMM YYYY")}</td>
            <td>
                <div>Current Address: {message.address}</div>
                <div>Original Address: {message.origin}</div>
                <div>Id : {id}</div>
                <p/>
                <div className={(expanded ? "expanded" : "not-expanded")}
                     onClick={(event) => {
                         event.stopPropagation()
                         dispatch(expandMessage(id))
                     }}> {(expanded ? String.fromCharCode('9660') : String.fromCharCode('9654'))}
                    &nbsp;&nbsp;Message
                    Body: {JSON.parse(JSON.stringify(message.messageBody, null, 2))}</div>
            </td>
        </tr>
    )
}