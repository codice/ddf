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
import {render} from "react-dom"
import store from './store'
import {
    isAllSelected,
    displayNumSelected,
    getSelectedIds,
    getSelectedMessages,
    getIds
} from './reducer'
import {
    addMessage,
    checkAllMessages,
    togglePolling,
    expandAllMessages,
    updateDeleted,
    removeMessages
} from './actions'
import TableView from './table.view'

require('es6-promise').polyfill()
require('isomorphic-fetch')

const getMessagesURI = '/admin/jolokia/exec/mil.af.gdes.message.broker.UndeliveredMessages:service=UndeliveredMessages/getMessages/jms.queue.DLQ/Core/'

const Json = ({value}) => (
    <pre>{JSON.stringify(value, null, 2)}</pre>
)

const retrieveData = (data, url) => {
    return window.fetch(url, {credentials: 'same-origin'})
        .then((res) => res.json())
        .then((json) => {
                json.value.filter((message) => !(getIds(data).includes(message.messageID))).map((message) => {
                    store.dispatch(addMessage({
                        timestamp: message.timestamp,
                        id: message.messageID,
                        expanded: false,
                        message: {
                            messageBody: message.body,
                            origin: message.StringProperties._AMQ_ORIG_QUEUE,
                            address: message.address
                        }
                    }))
                })
            }
        )
}

const operateOnData = (data, method) => {
    var ids = (method === 'resendMessages') ? getSelectedIds(getSelectedMessages(data).filter((message) => message.message.origin.length !== 0)) : getSelectedIds(data)
    window.fetch("/admin/jolokia/", {
        method: "POST",
        credentials: "same-origin",
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            type: "EXEC",
            mbean: "mil.af.gdes.message.broker.UndeliveredMessages:service=UndeliveredMessages",
            operation: method,
            arguments: ["jms.queue.DLQ", "Core", ids]
        })
    })
        .then((res) => {
            var x = res.json()
            console.log(x)
            return x
        })
        .then((json) => {
                if (json.value) {
                    store.dispatch(removeMessages(ids))
                    if (method === 'deleteMessages') {
                        store.dispatch(updateDeleted(json.value))
                    }


                }
            }
        )
        .catch()

}

const Header = ({data}) => {
    const checked = isAllSelected(data)
    return (
        <div>

            <button
                onClick={() => retrieveData(data, getMessagesURI)
                }> RETRIEVE
            </button>
            <button
                onClick={() => operateOnData(data, 'resendMessages')
                }> RESEND
            </button>
            <button
                onClick={() => operateOnData(data, 'deleteMessages')
                }> DELETE
            </button>
            <button onClick={() => poll()
            }> {store.getState().polling.polling ? 'STOP POLLING' : 'START POLLING'}
            </button>
            <div className="selected"> {displayNumSelected(data)} <span className="clickable"
                                                                        onClick={() => store.dispatch(checkAllMessages(true))}>Select All</span>
                <span className="clickable"
                      onClick={() => store.dispatch(checkAllMessages(false))}>Deselect All</span>
                <span className="clickable"
                      onClick={() => store.dispatch(expandAllMessages(true))}>Expand All</span>
                <span className="clickable"
                      onClick={() => store.dispatch(expandAllMessages(false))}>Collapse All</span>
                <span> {store.getState().operationStatus} messages deleted </span>
            </div>


            <div className="container">
                <TableView data={data} checked={checked}/>
            </div>

        </div>
    )
}

const renderView = () => render(
    <Header data={store.getState().messages}/>
    , document.getElementById('root'))


const poll = () => {
    if (store.getState().polling.polling) {
        clearInterval(store.getState().polling.id)
        store.dispatch(togglePolling())
    } else {
        store.dispatch(togglePolling(setInterval(() => {
            retrieveData(store.getState().messages, getMessagesURI)
        }, 5000)))
    }
}

store.subscribe(renderView)
renderView()