import React from 'react'
import { connect } from 'react-redux'

import { getException } from '../reducer'

import * as styles from './exceptions.less'

import { Tabs, Tab } from 'material-ui/Tabs'

import { clearException } from '../fetch'

import Http from 'material-ui/svg-icons/action/http'
import Code from 'material-ui/svg-icons/action/code'

import FloatingActionButton from 'material-ui/FloatingActionButton'

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

const Exception = ({ cause, stackTrace, method, url, body, onClear }) => {
  if (cause === undefined && stackTrace === undefined) return null

  const closeStyle = {
    position: 'absolute',
    left: 25,
    top: -25,
    zIndex: 9001
  }

  return (
    <div className={styles.exception}>
      <FloatingActionButton style={closeStyle} onClick={onClear} secondary>
        <span>&times;</span>
      </FloatingActionButton>

      <Tabs>
        <Tab label='Stacktrace' icon={<Code />}>
          <div className={styles.message}>
            <div>{cause}</div>
            <PrettyStackTrace lines={stackTrace} />
          </div>
        </Tab>
        <Tab label='Request' icon={<Http />}>
          <div className={styles.message}>
            <div>{method} {url}</div>
            <pre>{JSON.stringify(JSON.parse(body), null, 2)}</pre>
          </div>
        </Tab>
      </Tabs>
    </div>
  )
}

const mapStateToProps = (state) => getException(state) || {}

export default connect(mapStateToProps, { onClear: clearException })(Exception)
