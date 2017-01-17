import React from 'react'

import { connect } from 'react-redux'

import { getConfig } from '../../../reducer'

import { editConfig, testConfig } from '../../actions'

import {
  Stage,
  StageControls,
  Title,
  Description,
  Back
} from '../../components/stage'

import {
  setMappingToAdd,
  addMapping,
  setSelectedMappings,
  removeSelectedMappings
} from '../actions'

import { Card, CardHeader } from 'material-ui/Card'

import {
  Table,
  TableBody,
  TableHeader,
  TableHeaderColumn,
  TableRow,
  TableRowColumn
} from 'material-ui/Table'

import SelectField from 'material-ui/SelectField'
import MenuItem from 'material-ui/MenuItem'
import RaisedButton from 'material-ui/RaisedButton'

const LdapAttributeMappingStage = (props) => {
  const {
    id,
    disabled,
    tableMappings = [],
    subjectClaims = [],
    userAttributes = [],
    mappingToadd,

    // actions
    setMappingToAdd,
    addMapping,
    setSelectedMappings,
    removeSelectedMappings
  } = props

  return (
    <Stage id={id} probeUrl='/admin/beta/config/probe/ldap/subject-attributes'>
      <Title>LDAP User Attribute Mapping</Title>
      <Description>
        In order to authenticate users, the attributes of the users must be mapped to the STS
        claims.
        Not all attributes must be mapped but any unmapped attributes will not be used for
        authentication.
        Claims can be mapped to 1 or more attributes.
      </Description>
      <SelectField floatingLabelText='STS Claim'
        value={mappingToadd.subjectClaim}
        style={{width: '100%', clear: 'both'}}
        onChange={(e, i) => setMappingToAdd({subjectClaim: subjectClaims[i]})}>
        {subjectClaims.map((claim, i) => <MenuItem key={i} value={claim} primaryText={claim} />)}
      </SelectField>
      <SelectField floatingLabelText='LDAP User Attribute'
        value={mappingToadd.userAttribute}
        style={{width: '100%', clear: 'both'}}
        onChange={(e, i) => setMappingToAdd({userAttribute: userAttributes[i]})}>
        {userAttributes.map((attri, i) => <MenuItem key={i} value={attri}
          primaryText={attri} />)}
      </SelectField>
      <RaisedButton
        label='Add Mapping'
        primary
        disabled={mappingToadd.subjectClaim === undefined || mappingToadd.userAttribute === undefined}
        style={{margin: '0 auto', marginBottom: '30px', marginTop: '10px', display: 'block'}}
        onClick={() => addMapping(mappingToadd)} />
      <Card expanded style={{ width: '100%' }}>
        <CardHeader style={{ fontSize: '0.80em' }}>
          <Title>STS Claims to LDAP Attribute Mapping</Title>
          <Description>
            The mappings below will be saved.
          </Description>
        </CardHeader>
        <Table onRowSelection={(indexs) => setSelectedMappings(indexs)}
          multiSelectable>
          <TableHeader displaySelectAll={false} adjustForCheckbox={false}>
            <TableRow >
              <TableHeaderColumn>STS Claim</TableHeaderColumn>
              <TableHeaderColumn style={{ width: 120 }}>LDAP User Attribute</TableHeaderColumn>
            </TableRow>
          </TableHeader>
          <TableBody showRowHover deselectOnClickaway={false}>
            {tableMappings.map((mapping, i) =>
              <TableRow key={i} selected={mapping.selected}>
                <TableRowColumn>
                  <span style={{cursor: 'help'}} title={mapping.subjectClaim}>{mapping.subjectClaim}</span>
                </TableRowColumn>
                <TableRowColumn style={{ width: 120 }}>{mapping.userAttribute}</TableRowColumn>
              </TableRow>)}
          </TableBody>
        </Table>
        <RaisedButton
          label='Remove Selected Mappings'
          primary
          style={{display: 'block'}}
          disabled={tableMappings.filter((mapping) => mapping.selected).length === 0}
          onClick={() => removeSelectedMappings()} />
      </Card>
      <StageControls>
        <Back disabled={disabled} />
        <NextAttributeMapping id={id} disabled={disabled || tableMappings.length === 0} url='/admin/beta/config/test/ldap/attribute-mapping' attributeMappings={toAttributeMapping(tableMappings)}
          nextStageId='confirm' />
      </StageControls>
    </Stage>
  )
}

const toAttributeMapping = (tableMappings) => {
  return (tableMappings.length !== 0) ? tableMappings.reduce((prevObj, mapping) => {
    prevObj[mapping.subjectClaim] = mapping.userAttribute
    return prevObj
  }, {}) : {}
}
// todo Need to transform map into a string, list map
const mapDispatchToPropsNextAttributeMapping = (dispatch, {id, url, nextStageId, attributeMappings}) => ({
  next: () => { dispatch(editConfig('attributeMappings', attributeMappings)); dispatch(testConfig(id, url, nextStageId)) }
})
const NextAttributeMappingView = ({next, disabled, nextStageId}) => <RaisedButton label='Next' disabled={disabled} primary onClick={next} />
const NextAttributeMapping = connect(null, mapDispatchToPropsNextAttributeMapping)(NextAttributeMappingView)

const getSubjectClaims = (state) => (getConfig(state, 'subjectClaims') !== undefined ? getConfig(state, 'subjectClaims').options : undefined)
const getUserAttributes = (state) => (getConfig(state, 'userAttributes') !== undefined ? getConfig(state, 'userAttributes').options : undefined)
const getTableMappings = (state) => state.getIn(['wizard', 'tableMappings'])
const getMappingToAdd = (state) => state.getIn(['wizard', 'mappingToAdd'])

export default connect(
  (state) => ({
    subjectClaims: getSubjectClaims(state),
    userAttributes: getUserAttributes(state),
    tableMappings: getTableMappings(state),
    mappingToadd: getMappingToAdd(state)
  }),

  {
    setMappingToAdd,
    addMapping,
    setSelectedMappings,
    removeSelectedMappings
  }
)(LdapAttributeMappingStage)
