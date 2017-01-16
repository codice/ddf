import React from 'react'

import { connect } from 'react-redux'

import { getProbeValue, getConfig } from '../../../reducer'

import { List, ListItem } from 'material-ui/List'
import { Card, CardActions, CardHeader } from 'material-ui/Card'
import FlatButton from 'material-ui/FlatButton'

import {
  Stage,
  StageControls,
  Title,
  Description,
  Back,
  Next
} from '../../components/stage'

import { InputAuto } from '../../inputs'

import { probe } from '../actions'

import * as styles from '../styles.less'

const getLdapUseCase = (state) => {
  const useCase = getConfig(state, 'ldapUseCase')
  if (useCase !== undefined) {
    return useCase.value
  }
}

const QueryResult = (props) => {
  const {name, uid, cn, ou} = props

  return (
    <ListItem
      primaryText={ou || cn || uid || name}
      nestedItems={[
        <pre className={styles.queryResultStyle} key={1}>{JSON.stringify(props, null, 2)}</pre>
      ]}
      primaryTogglesNestedList
    />
  )
}

const DirectorySettings = ({ probe, probeValue = [], id, disabled, ldapUseCase }) => (
  <Stage id={id} probeUrl='/admin/beta/config/probe/ldap/dir-struct'>
    <Title>LDAP Directory Structure</Title>
    <Description>
      Next we need to configure the directories to for users/members and the attributes to use.
      Below is the LDAP Query Tool, capable of executing queries against the connected LDAP to discover the required field values
    </Description>
    <InputAuto id='baseUserDn' disabled={disabled} label='Base User DN' />
    <InputAuto id='userNameAttribute' disabled={disabled} label='User Name Attribute' />
    <InputAuto id='baseGroupDn' disabled={disabled} label='Base Group DN' />
    {ldapUseCase === 'loginAndCredentialStore' || ldapUseCase === 'credentialStore'
      ? <div>
        <InputAuto id='groupObjectClass' disabled={disabled} label='LDAP Group ObjectClass' />
        <InputAuto id='membershipAttribute' disabled={disabled} label='LDAP Membership Attribute' />
      </div>
      : null}
    <Card >
      <CardHeader style={{textAlign: 'center', fontSize: '1.1em'}}
        title='LDAP Query Tool'
        subtitle='Execute queries against the connected LDAP'
        actAsExpander
        showExpandableButton
      />
      <CardActions expandable style={{margin: '5px'}}>
        <InputAuto id='query' disabled={disabled} label='Query' />
        <InputAuto id='queryBase' disabled={disabled} label='Query Base DN' />

        <div style={{textAlign: 'right', marginTop: 20}}>
          <FlatButton disabled={disabled} secondary label='run query' onClick={() => probe('/admin/beta/config/probe/ldap/query')} />
        </div>

        {probeValue.length === 0
         ? null
         : (<div className={styles.queryWindow}>
           <h2 className={styles.title}>Query Results</h2>
           <List>
             {probeValue.map((v, i) => <QueryResult key={i} {...v} />)}
           </List>
         </div>)}
      </CardActions>
    </Card>

    <StageControls>
      <Back disabled={disabled} />
      {ldapUseCase === 'loginAndCredentialStore' || ldapUseCase === 'credentialStore'
        ? (<Next id={id} disabled={disabled}
          url='/admin/beta/config/test/ldap/dir-struct'
          nextStageId='attribute-mapping' />)
        : (<Next id={id}
          disabled={disabled}
          url='/admin/beta/config/test/ldap/dir-struct'
          nextStageId='confirm' />)}
    </StageControls>
  </Stage>
)

export default connect(
  (state) => ({probeValue: getProbeValue(state), ldapUseCase: getLdapUseCase(state)}),
  { probe }
)(DirectorySettings)

