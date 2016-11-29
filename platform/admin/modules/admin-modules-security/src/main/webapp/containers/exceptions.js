import React from 'react'
import { connect } from 'react-redux'

import { getBackendErrors } from '../reducer'

import { exception, message } from './exceptions.less'

import { Tabs, Tab } from 'material-ui/Tabs'

import Http from 'material-ui/svg-icons/action/http'
import Code from 'material-ui/svg-icons/action/code'

const PrettyStackLine = ({ declaringClass, methodName, fileName, lineNumber }) => (
  <div>
    <a href={'http://localhost:8091?message=' + fileName + ':' + lineNumber}>at {declaringClass}.{methodName}({fileName}:{lineNumber})</a>
  </div>
)

const PrettyStackTrace = ({ lines }) => (
  <div>
    {lines.map((line, i) => <PrettyStackLine key={i} {...line} />)}
  </div>
)

const Exception = ({ cause, stackTrace, method, url, body }) => {
  if (cause === undefined && stackTrace === undefined) return null

  return (
    <div className={exception}>

      <Tabs>
        <Tab label='Stacktrace' icon={<Code />}>
          <div className={message}>
            <div>{cause}</div>
            <PrettyStackTrace lines={stackTrace} />
          </div>
        </Tab>
        <Tab label='Request' icon={<Http />}>
          <div className={message}>
            <div>{method} {url}</div>
            <pre>{JSON.stringify(body, null, 2)}</pre>
          </div>
        </Tab>
      </Tabs>

    </div>
  )
}

const mapStateToProps = (state) => getBackendErrors(state)

export default connect(mapStateToProps)(Exception)
