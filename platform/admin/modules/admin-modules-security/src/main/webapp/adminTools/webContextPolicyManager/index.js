import React from 'react'
import Paper from 'material-ui/Paper'

import { connect } from 'react-redux'

import Mount from '../../components/mount'
import {
  getBins,
  getOptions
} from '../../reducer'

import {
  removeBin,
  addAttribute,
  editAttribute,
  removeAttribute,
  addNewBin,
  editModeOn,
  editModeCancel,
  editModeSave,
  editRealm,
  updatePolicyBins,
  persistChanges,
  addAttributeMapping
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
import SaveIcon from 'material-ui/svg-icons/content/save'

import {
  contextPolicyStyle,
  infoTitle,
  infoSubtitle,
  policyBinOuterStyle,
  editPaneStyle,
  newBinStyle,
  infoSubtitleLeft,
  realmNameStyle
} from './styles.less'

export const InfoField = ({ id, label, value }) => (
  <div style={{ fontSize: '16px', lineHeight: '24px', width: '100%', display: 'inline-block', position: 'relative', height: '200ms' }}>
    <label htmlFor={id} style={{ color: 'rgba(0, 0, 0, 0.541176)', position: 'absolute', lineHeight: '22px', top: '30px', transform: 'scale(0.75) translate(10px, -28px)', transformOrigin: 'left top 0px' }}>{label}</label>
    <p id={id} style={{ position: 'relative', height: '100%', margin: '28px 10px 7px', whiteSpace: 'nowrap' }}>{value}</p>
  </div>
)

let Info = ({id, name, realm, authenticationTypes, requiredAttributes, binNumber, editModeOn}) => (
  <div style={{ width: '100%' }}>
    <Flexbox className={editPaneStyle} justifyContent='center' alignItems='center' onClick={editModeOn}>
      <IconButton tooltip={'Edit Attributes'} tooltipPosition='top-center'><EditModeIcon /></IconButton>
    </Flexbox>
  </div>
)
Info = connect(null, (dispatch, { binNumber }) => ({ editModeOn: () => dispatch(editModeOn(binNumber)) }))(Info)

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

const PolicyBin = ({ policyBin, binNumber }) => (
  <Paper className={policyBinOuterStyle} >
    <Flexbox flexDirection='row'>
      <Flexbox style={{ width: '20%', padding: '5px', borderRight: '1px solid grey' }} flexDirection='column'>
        <p className={infoSubtitleLeft}>Context Paths</p>
        {policyBin.contextPaths.map((contextPath, pathNumber) => (<ContextPathItem contextPath={contextPath} key={pathNumber} binNumber={binNumber} pathNumber={pathNumber} />))}
        <Divider />
      </Flexbox>
      <Flexbox style={{ width: '20%', padding: '5px' }} flexDirection='column'>
        <p className={infoSubtitleLeft}>Realm</p>
        <Divider />
        <p className={realmNameStyle}>{policyBin.realm}</p>
        <p className={infoSubtitleLeft}>Authentication Types</p>
        <Flexbox flexDirection='column'>
          {policyBin.authenticationTypes.map((authType, pathNumber) => (<ContextPathItem editing={false} attribute='authenticationTypes' key={pathNumber} contextPath={authType} binNumber={binNumber} pathNumber={pathNumber} />))}
        </Flexbox>
      </Flexbox>
      <Flexbox style={{ width: '60%', padding: '5px' }} flexDirection='column'>
        <p className={infoSubtitleLeft}>Required Attributes</p>
        <Divider />
        <Flexbox flexDirection='column'>
          <Table>
            <TableHeader style={{ padding: '1px' }} displaySelectAll={false} adjustForCheckbox={false}>
              <TableRow>
                <TableHeaderColumn>STS Claim</TableHeaderColumn>
                <TableHeaderColumn style={{ width: 120 }}>Attribute Mapping</TableHeaderColumn>
              </TableRow>
            </TableHeader>
            <TableBody displayRowCheckbox={false}>
              {Object.keys(policyBin.requiredAttributes).map((key, i) =>
                <TableRow key={i}>
                  <TableRowColumn>
                    <span style={{cursor: 'help'}}>{key}</span>
                  </TableRowColumn>
                  <TableRowColumn style={{ width: 120 }}>{policyBin.requiredAttributes[key]}</TableRowColumn>
                </TableRow>)}
            </TableBody>
          </Table>
        </Flexbox>
      </Flexbox>
    </Flexbox>
    <Info name={policyBin.name} realm={policyBin.realm} binNumber={binNumber} authenticationTypes={policyBin.authenticationTypes} requiredAttributes={policyBin.requiredAttributes} />
  </Paper>
)

let EditPolicyBin = ({ policyBin, policyOptions, binNumber, removeBin, editModeSave, editModeCancel, editRealm, editName, addAttributeMapping, editAttribute, removeAttribute }) => (
  <Paper className={policyBinOuterStyle} >
    <Flexbox flexDirection='row'>
      <Flexbox style={{ width: '20%', padding: '5px', borderRight: '1px solid grey' }} flexDirection='column'>
        <p className={infoSubtitleLeft}>Context Paths</p>
        {policyBin.contextPaths.map((contextPath, pathNumber) => (<ContextPathItem editing attribute='contextPaths' contextPath={contextPath} key={pathNumber} binNumber={binNumber} pathNumber={pathNumber} />))}
        <NewContextPathItem binNumber={binNumber} attribute='contextPaths' newPath={policyBin['newcontextPaths']} />
      </Flexbox>
      <Flexbox style={{ width: '20%', padding: '5px' }} flexDirection='column'>
        <p className={infoSubtitleLeft}>Realm</p>
        <Divider />
        <Flexbox flexDirection='row'>
          <SelectField fullWidth style={{ margin: '0px 10px' }} id='realm' value={policyBin.realm} onChange={(event, i, value) => editRealm(value)}>
            {policyOptions.realms.map((realm, i) => (<MenuItem value={realm} primaryText={realm} key={i} />))}
          </SelectField>
        </Flexbox>
        <p className={infoSubtitleLeft}>Authentication Types</p>
        <Flexbox flexDirection='column'>
          {policyBin.authenticationTypes.map((contextPath, pathNumber) => (<ContextPathItem editing attribute='authenticationTypes' contextPath={contextPath} key={pathNumber} binNumber={binNumber} pathNumber={pathNumber} />))}
          <NewSelectItem binNumber={binNumber} attribute='authenticationTypes' options={policyOptions.authenticationTypes.filter((option) => !policyBin.authenticationTypes.includes(option))} newPath={policyBin['newauthenticationTypes']} />
        </Flexbox>
      </Flexbox>
      <Flexbox style={{ width: '60%', padding: '5px' }} flexDirection='column'>
        <p className={infoSubtitleLeft}>Required Attributes</p>
        <Divider />
        <Flexbox flexDirection='column'>
          <Table selectable={false}>
            <TableHeader displaySelectAll={false} adjustForCheckbox={false}>
              <TableRow>
                <TableHeaderColumn>STS Claim</TableHeaderColumn>
                <TableHeaderColumn style={{ width: 120 }}>Attribute Mapping</TableHeaderColumn>
              </TableRow>
            </TableHeader>
            <TableBody displayRowCheckbox={false}>
              {Object.keys(policyBin.requiredAttributes).map((key, i) =>
                <TableRow key={i}>
                  <TableRowColumn>
                    <span style={{cursor: 'help'}}>{key}</span>
                  </TableRowColumn>
                  <TableRowColumn style={{ width: 120, position: 'relative' }}>
                    <span>{policyBin.requiredAttributes[key]}</span>
                    <IconButton style={{ position: 'absolute', right: 0, top: 0 }} tooltip={'Add This Path'} tooltipPosition='top-center' onClick={() => removeAttribute('requiredAttributes', i)}><CancelIcon /></IconButton>
                  </TableRowColumn>
                </TableRow>)}
              <TableRow>
                <TableRowColumn>
                  <SelectField style={{ margin: '0px', width: '100%', fontSize: '14px' }} id='claims' value={policyBin.newrequiredClaim || ''} onChange={(event, i, value) => editAttribute('requiredClaim', value)}>
                    {policyOptions.claims.map((claim, i) => (<MenuItem style={{ fontSize: '12px' }} value={claim} primaryText={claim} key={i} />))}
                  </SelectField>
                </TableRowColumn>
                <TableRowColumn style={{ width: 120, position: 'relative' }}>
                  <TextField fullWidth style={{ margin: '0px', fontSize: '14px' }} id='claims' value={policyBin.newrequiredAttribute || ''} onChange={(event, value) => editAttribute('requiredAttribute', value)} />
                  <IconButton style={{ position: 'absolute', right: 0, top: 0 }} tooltip={'Add Item'} tooltipPosition='top-center' onClick={addAttributeMapping}><AddIcon color={cyanA700} /></IconButton>
                </TableRowColumn>
              </TableRow>
            </TableBody>
          </Table>
        </Flexbox>
      </Flexbox>
    </Flexbox>
    <Divider />
    <Flexbox flexDirection='row' justifyContent='center' style={{ padding: '10px 0px 5px' }}>
      <FlatButton style={{ margin: '0 10' }} label='Cancel' labelPosition='after' secondary onClick={editModeCancel} />
      <RaisedButton style={{ margin: '0 10' }} label='Done' primary onClick={editModeSave} />
      <IconButton style={{ position: 'absolute', right: '0px', bottom: '0px' }} onClick={removeBin} tooltip={'Delete'} tooltipPosition='top-center' ><DeleteIcon /></IconButton>
    </Flexbox>
  </Paper>
)
EditPolicyBin = connect(
  (state) => ({
    policyOptions: getOptions(state)
  }),
  (dispatch, { binNumber }) => ({
    removeBin: () => dispatch(removeBin(binNumber)),
    editModeSave: () => dispatch(editModeSave(binNumber)),
    editModeCancel: () => dispatch(editModeCancel(binNumber)),
    editRealm: (value) => dispatch(editRealm(binNumber, value)),
    addAttributeMapping: () => dispatch(addAttributeMapping(binNumber)),
    editAttribute: (attribute, value) => dispatch(editAttribute(attribute)(binNumber, value)),
    removeAttribute: (attribute, pathNumber) => dispatch(removeAttribute(attribute)(binNumber, pathNumber))
  }))(EditPolicyBin)

let PolicyBins = ({ policies }) => (
  <Flexbox style={{ height: '100%', width: '100%', overflowY: 'scroll', padding: '0px 5px', boxSizing: 'border-box' }} flexDirection='column' alignItems='center' >
    { policies.map((policyBin, binNumber) => {
      if (policyBin.editing !== undefined && policyBin.editing) {
        return (<EditPolicyBin policyBin={policyBin} key={binNumber} binNumber={binNumber} />)
      }
      return (<PolicyBin policyBin={policyBin} key={binNumber} binNumber={binNumber} />)
    })}
    <NewBin />
  </Flexbox>
)
PolicyBins = connect((state) => ({ policies: getBins(state) }))(PolicyBins)

let NewBin = ({ policies, addNewBin }) => (
  <Paper style={{ position: 'relative', width: '100%', height: '100px', margin: '5px 0px', textAlign: 'center', backgroundColor: '#EEE' }} onClick={addNewBin}>
    <Flexbox className={newBinStyle} flexDirection='column' justifyContent='center' alignItems='center'>
      <FloatingActionButton>
        <ContentAdd />
      </FloatingActionButton>
    </Flexbox>
  </Paper>
)
NewBin = connect(null, { addNewBin })(NewBin)

let wcpm = ({ updatePolicyBins, persistChanges }) => (
  <Mount on={updatePolicyBins('/admin/wizard/configurations/contextPolicyManager')}>
    <Flexbox flexDirection='column' style={{ width: '100%', height: '100%' }}>
      <Paper style={{ backgroundColor: '#EEE', width: '100%' }}>
        <p className={infoTitle}>Web Context Policy Manager</p>
        <p className={infoSubtitle}>This is the web context policy manager. Hopefully it's nicer to use than the other one.</p>
      </Paper>
      <Divider />
      <PolicyBins />
      <FloatingActionButton style={{ position: 'absolute', right: 20, bottom: 20 }} onClick={() => persistChanges('/admin/wizard/persist/contextPolicyManager')}>
        <SaveIcon />
      </FloatingActionButton>
    </Flexbox>
  </Mount>
)
export default connect(null, { updatePolicyBins, persistChanges })(wcpm)
