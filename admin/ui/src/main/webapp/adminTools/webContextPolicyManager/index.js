import React from 'react'
import Paper from 'material-ui/Paper'

import { connect } from 'react-redux'

import Mount from '../../components/mount'
import {
  getBins,
  getOptions,
  getEditingBinNumber,
  getConfirmDelete
} from '../../reducer'

import {
  removeBin,
  addAttribute,
  editAttribute,
  removeAttribute,
  addNewBin,
  editModeOn,
  editModeCancel,
  editRealm,
  updatePolicyBins,
  persistChanges,
  addAttributeMapping,
  removeAttributeMapping,
  confirmRemoveBinAndPersist,
  cancelRemoveBin
} from './actions'

import Flexbox from 'flexbox-react'
import IconButton from 'material-ui/IconButton'
import TextField from 'material-ui/TextField'
import Divider from 'material-ui/Divider'
import FloatingActionButton from 'material-ui/FloatingActionButton'
import SelectField from 'material-ui/SelectField'
import MenuItem from 'material-ui/MenuItem'
import FlatButton from 'material-ui/FlatButton'
import RaisedButton from 'material-ui/RaisedButton'
import {Table, TableBody, TableHeader, TableHeaderColumn, TableRow, TableRowColumn} from 'material-ui/Table'

import { cyanA700 } from 'material-ui/styles/colors'

import CancelIcon from 'material-ui/svg-icons/content/remove-circle-outline'
import DeleteIcon from 'material-ui/svg-icons/action/delete'
import AddIcon from 'material-ui/svg-icons/content/add-circle-outline'
import ContentAdd from 'material-ui/svg-icons/content/add'
import EditModeIcon from 'material-ui/svg-icons/editor/mode-edit'

import {
  contextPolicyStyle,
  infoTitle,
  infoSubtitle,
  policyBinOuterStyle,
  editPaneStyle,
  newBinStyle,
  infoSubtitleLeft,
  realmNameStyle,
  disabledPanelStyle,
  newBinDisabledStyle,
  contextPathGroupStyle,
  whitelistContextPathGroupStyle
} from './styles.less'

let Edit = ({editing, binNumber, editModeOn}) => {
  return !editing ? (
    <div style={{ width: '100%' }}>
      <Flexbox className={editPaneStyle} justifyContent='flex-end' alignItems='flex-start'>
        <FloatingActionButton onClick={editModeOn}><EditModeIcon /></FloatingActionButton>
      </Flexbox>
    </div>
  ) : null
}

Edit = connect(null, (dispatch, { binNumber }) => ({ editModeOn: () => dispatch(editModeOn(binNumber)) }))(Edit)

let ContextPathItem = ({ contextPath, binNumber, pathNumber, removePath, editing, attribute }) => (
  <div>
    <Divider />
    <Flexbox flexDirection='row' justifyContent='space-between'>
      <span className={contextPolicyStyle}>{contextPath}</span>
      {editing ? (<IconButton tooltip={'Remove'} tooltipPosition='top-center' onClick={removePath}><CancelIcon /></IconButton>) : null}
    </Flexbox>
  </div>
)
ContextPathItem = connect(null, (dispatch, { binNumber, pathNumber, attribute }) => ({ removePath: () => dispatch(removeAttribute(attribute)(binNumber, pathNumber)) }))(ContextPathItem)

let NewContextPathItem = ({ binNumber, addPath, onEdit, newPath, attribute, addButtonVisible = true }) => (
  <div>
    <Divider />
    <Flexbox flexDirection='row' justifyContent='space-between'>
      <TextField fullWidth style={{ paddingLeft: '10px' }} id='name' hintText='Add New Path' onChange={(event, value) => onEdit(value)} value={newPath || ''} />
      {(addButtonVisible) ? (<IconButton tooltip={'Add'} tooltipPosition='top-center' onClick={addPath}><AddIcon color={cyanA700} /></IconButton>) : null }
    </Flexbox>
  </div>
)
NewContextPathItem = connect(null, (dispatch, { binNumber, attribute }) => ({
  addPath: () => dispatch(addAttribute(attribute)(binNumber)),
  onEdit: (value) => dispatch(editAttribute(attribute)(binNumber, value))
}))(NewContextPathItem)

let ContextPathGroup = ({ bin, binNumber, editing }) => (
  <Flexbox className={(bin.name === 'WHITELIST') ? whitelistContextPathGroupStyle : contextPathGroupStyle} flexDirection='column'>
    <p className={infoSubtitleLeft}>Context Paths</p>
    {bin.contextPaths.map((contextPath, pathNumber) => (<ContextPathItem attribute='contextPaths' contextPath={contextPath} key={pathNumber} binNumber={binNumber} pathNumber={pathNumber} editing={editing} />))}
    {editing ? <NewContextPathItem binNumber={binNumber} attribute='contextPaths' newPath={bin['newcontextPaths']} /> : null}
    <Divider />
  </Flexbox>
)

let NewSelectItem = ({ binNumber, addPath, onEdit, newPath, attribute, options, addButtonVisible = true }) => (
  <div>
    <Divider />
    <Flexbox flexDirection='row' justifyContent='space-between'>
      <SelectField fullWidth style={{ paddingLeft: '10px' }} id='name' hintText='Add New Path' onChange={(event, i, value) => onEdit(value)} value={newPath || ''}>
        { options.map((item, key) => (<MenuItem value={item} key={key} primaryText={item} />)) }
      </SelectField>
      {(addButtonVisible) ? (<IconButton tooltip={'Add Item'} tooltipPosition='top-center' onClick={addPath}><AddIcon color={cyanA700} /></IconButton>) : null }
    </Flexbox>
  </div>
)
NewSelectItem = connect(null, (dispatch, { binNumber, attribute }) => ({
  addPath: () => dispatch(addAttribute(attribute)(binNumber)),
  onEdit: (value) => dispatch(editAttribute(attribute)(binNumber, value))
}))(NewSelectItem)

let Realm = ({ bin, binNumber, policyOptions, editRealm, editing }) => {
  return editing ? (
    <Flexbox flexDirection='row'>
      <SelectField fullWidth style={{margin: '0px 10px'}} id='realm' value={bin.realm} onChange={(event, i, value) => editRealm(value)}>
        {policyOptions.realms.map((realm, i) => (<MenuItem value={realm} primaryText={realm} key={i} />))}
      </SelectField>
    </Flexbox>
  ) : (
    <p className={realmNameStyle}>{bin.realm}</p>
  )
}
Realm = connect(
  (state) => ({
    policyOptions: getOptions(state)
  }),
  (dispatch, { binNumber }) => ({
    editRealm: (value) => dispatch(editRealm(binNumber, value))
  }))(Realm)

let ConfirmationPanel = ({ bin, binNumber, removeBin, saveAndPersist, editModeCancel, editing, confirmRemoveBinAndPersist, confirmDelete, cancelRemoveBin, allowDelete }) => {
  return editing ? (
    <Flexbox flexDirection='row' justifyContent='center' style={{ padding: '10px 0px 5px' }}>
      <FlatButton style={{ margin: '0 10' }} label='Cancel' labelPosition='after' secondary onClick={editModeCancel} />
      <RaisedButton style={{ margin: '0 10' }} label='Save' primary onClick={saveAndPersist} />
      {
        (confirmDelete && allowDelete) ? (
          <Flexbox style={{ position: 'absolute', right: '0px', bottom: '0px', margin: '5px' }} flexDirection='column' alignItems='center' >
            <p className={infoSubtitleLeft}>Confirm delete all?</p>
            <Flexbox flexDirection='row'>
              <RaisedButton label='Yes' primary onClick={confirmRemoveBinAndPersist} />
              <RaisedButton label='No' secondary onClick={cancelRemoveBin} />
            </Flexbox>
          </Flexbox>
        ) : (
          (allowDelete) ? <IconButton style={{ position: 'absolute', right: '0px', bottom: '0px' }} onClick={removeBin} tooltip={'Delete'} tooltipPosition='top-center' ><DeleteIcon /></IconButton> : null
        )
      }
    </Flexbox>
  ) : null
}

ConfirmationPanel = connect((state) => ({
  confirmDelete: getConfirmDelete(state)
}), (dispatch, { binNumber }) => ({
  removeBin: () => dispatch(removeBin(binNumber)),
  cancelRemoveBin: () => dispatch(cancelRemoveBin()),
  saveAndPersist: () => dispatch(persistChanges(binNumber, '/admin/beta/config/persist/context-policy-manager/edit')),
  editModeCancel: () => dispatch(editModeCancel(binNumber)),
  confirmRemoveBinAndPersist: () => dispatch(confirmRemoveBinAndPersist(binNumber, '/admin/beta/config/persist/context-policy-manager/edit'))
}))(ConfirmationPanel)

let AuthTypesGroup = ({ bin, binNumber, policyOptions, editing }) => (
  <Flexbox flexDirection='column'>
    {bin.authenticationTypes.map((contextPath, pathNumber) => (<ContextPathItem attribute='authenticationTypes' contextPath={contextPath} key={pathNumber} binNumber={binNumber} pathNumber={pathNumber} editing={editing} />))}
    {editing ? (
      <NewSelectItem binNumber={binNumber} attribute='authenticationTypes' options={policyOptions.authenticationTypes.filter((option) => !bin.authenticationTypes.includes(option))} newPath={bin['newauthenticationTypes']} />
    ) : null }
  </Flexbox>
)
AuthTypesGroup = connect(
  (state) => ({
    policyOptions: getOptions(state)
  }))(AuthTypesGroup)

let AttributeTableGroup = ({ bin, binNumber, policyOptions, editAttribute, removeAttributeMapping, addAttributeMapping, editing }) => (
  <Table selectable={false}>
    <TableHeader displaySelectAll={false} adjustForCheckbox={false}>
      <TableRow>
        <TableHeaderColumn>STS Claim</TableHeaderColumn>
        <TableHeaderColumn style={{ width: 120 }}>Attribute Mapping</TableHeaderColumn>
      </TableRow>
    </TableHeader>
    <TableBody displayRowCheckbox={false}>
      {Object.keys(bin.requiredAttributes).map((key, i) =>
        <TableRow key={i}>
          <TableRowColumn>
            <span style={{cursor: 'help'}}>{key}</span>
          </TableRowColumn>
          <TableRowColumn style={{ width: 120, position: 'relative' }}>
            <span>{bin.requiredAttributes[key]}</span>
            {editing ? (
              <IconButton style={{ position: 'absolute', right: 0, top: 0 }} tooltip={'Add This Path'} tooltipPosition='top-center' onClick={() => removeAttributeMapping(key)}><CancelIcon /></IconButton>
            ) : null }
          </TableRowColumn>
        </TableRow>)}
      {editing ? (
        <TableRow>
          <TableRowColumn>
            <SelectField style={{ margin: '0px', width: '100%', fontSize: '14px' }} id='claims' value={bin.newrequiredClaim || ''} onChange={(event, i, value) => editAttribute('requiredClaim', value)}>
              {policyOptions.claims.map((claim, i) => (<MenuItem style={{ fontSize: '12px' }} value={claim} primaryText={claim} key={i} />))}
            </SelectField>
          </TableRowColumn>
          <TableRowColumn style={{ width: 120, position: 'relative' }}>
            <TextField fullWidth style={{ margin: '0px', fontSize: '14px' }} id='claims' value={bin.newrequiredAttribute || ''} onChange={(event, value) => editAttribute('requiredAttribute', value)} />
            <IconButton style={{ position: 'absolute', right: 0, top: 0 }} tooltip={'Add Item'} tooltipPosition='top-center' onClick={addAttributeMapping}><AddIcon color={cyanA700} /></IconButton>
          </TableRowColumn>
        </TableRow>
      ) : null }
    </TableBody>
  </Table>
)
AttributeTableGroup = connect(
  (state) => ({
    policyOptions: getOptions(state)
  }),
  (dispatch, { binNumber }) => ({
    addAttributeMapping: () => dispatch(addAttributeMapping(binNumber)),
    removeAttributeMapping: (claim) => dispatch(removeAttributeMapping(binNumber, claim)),
    editAttribute: (attribute, value) => dispatch(editAttribute(attribute)(binNumber, value))
  }))(AttributeTableGroup)

const DisabledPanel = () => (
  <div className={disabledPanelStyle} />
)

const WhitelistBin = ({ policyBin, binNumber, editing, editingBinNumber }) => (
  <Paper className={policyBinOuterStyle} >
    <Flexbox flexDirection='row'>
      <ContextPathGroup binNumber={binNumber} bin={policyBin} editing={editing} />
      <Flexbox style={{ padding: '5px' }} flexDirection='column' justifyContent='center'>
        <p className={infoTitle}>Whitelisted Contexts</p>
        <p className={infoSubtitle}>The contexts listed here will not be checked against any policies and all requests made to these endpoints will be permitted. Use with caution.</p>
      </Flexbox>
    </Flexbox>
    <Edit editing={editing} binNumber={binNumber} />
    <ConfirmationPanel bin={policyBin} binNumber={binNumber} editing={editing} allowDelete={false} />
    { (!editing && editingBinNumber !== null) ? <DisabledPanel /> : null }
  </Paper>
)

const PolicyBin = ({ policyBin, binNumber, editing, editingBinNumber }) => (
  <Paper className={policyBinOuterStyle} >
    <Flexbox flexDirection='row'>
      <ContextPathGroup binNumber={binNumber} bin={policyBin} editing={editing} />
      <Flexbox style={{ width: '20%', padding: '5px' }} flexDirection='column'>
        <p className={infoSubtitleLeft}>Realm</p>
        <Divider />
        <Realm bin={policyBin} binNumber={binNumber} editing={editing} />
        <p className={infoSubtitleLeft}>Authentication Types</p>
        <AuthTypesGroup bin={policyBin} binNumber={binNumber} editing={editing} />
      </Flexbox>
      <Flexbox style={{ width: '60%', padding: '5px' }} flexDirection='column'>
        <p className={infoSubtitleLeft}>Required Attributes</p>
        <Divider />
        <Flexbox flexDirection='column'>
          <AttributeTableGroup bin={policyBin} binNumber={binNumber} editing={editing} />
        </Flexbox>
      </Flexbox>
    </Flexbox>
    <Edit editing={editing} binNumber={binNumber} />
    <ConfirmationPanel bin={policyBin} binNumber={binNumber} editing={editing} allowDelete />
    { (!editing && editingBinNumber !== null) ? <DisabledPanel /> : null }
  </Paper>
)

let PolicyBins = ({ policies, editingBinNumber }) => (
  <Flexbox style={{ height: '100%', width: '100%', overflowY: 'scroll', padding: '0px 5px', boxSizing: 'border-box' }} flexDirection='column' alignItems='center' >
    { policies.map((policyBin, binNumber) => {
      return (policyBin.name === 'WHITELIST')
        ? (<WhitelistBin policyBin={policyBin} key={binNumber} binNumber={binNumber} editing={binNumber === editingBinNumber} editingBinNumber={editingBinNumber} />)
        : (<PolicyBin policyBin={policyBin} key={binNumber} binNumber={binNumber} editing={binNumber === editingBinNumber} editingBinNumber={editingBinNumber} />)
    })}
    <NewBin editing={editingBinNumber !== null} />
  </Flexbox>
)
PolicyBins = connect((state) => ({
  policies: getBins(state),
  editingBinNumber: getEditingBinNumber(state)
}))(PolicyBins)

let NewBin = ({ policies, addNewBin, nextBinNumber, editing }) => {
  if (editing) {
    return (
      <Paper style={{ position: 'relative', width: '100%', height: '100px', margin: '5px 0px', textAlign: 'center', backgroundColor: '#EEE' }} >
        <Flexbox className={newBinDisabledStyle} flexDirection='column' justifyContent='center' alignItems='center'>
          <FloatingActionButton disabled>
            <ContentAdd />
          </FloatingActionButton>
        </Flexbox>
      </Paper>
    )
  } else {
    return (
      <Paper style={{ position: 'relative', width: '100%', height: '100px', margin: '5px 0px', textAlign: 'center', backgroundColor: '#EEE' }} onClick={() => addNewBin(nextBinNumber)}>
        <Flexbox className={newBinStyle} flexDirection='column' justifyContent='center' alignItems='center'>
          <FloatingActionButton>
            <ContentAdd />
          </FloatingActionButton>
        </Flexbox>
      </Paper>
    )
  }
}
NewBin = connect((state) => ({ nextBinNumber: getBins(state).length }), { addNewBin })(NewBin)

let wcpm = ({ updatePolicyBins }) => (
  <Mount on={updatePolicyBins('/admin/beta/config/configurations/context-policy-manager')}>
    <Flexbox flexDirection='column' style={{ width: '100%', height: '100%' }}>
      <Paper style={{ backgroundColor: '#EEE', width: '100%' }}>
        <p className={infoTitle}>Web Context Policy Manager</p>
        <p className={infoSubtitle}>The Web Context Policy Manager defines all security policies for REST endpoints. It defines the realms a context should authenticate against, the type of authentication that a context requires, and any user attributes that are required for authorization.</p>
      </Paper>
      <Divider />
      <PolicyBins />
    </Flexbox>
  </Mount>
)
export default connect(null, { updatePolicyBins })(wcpm)
