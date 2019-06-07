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
import React from 'react'

import { Map, fromJS } from 'immutable'
import { connect } from 'react-redux'

import CircularProgress from 'material-ui/CircularProgress'
import ContentAdd from 'material-ui/svg-icons/content/add'
import DeleteIcon from 'material-ui/svg-icons/action/delete'
import WarningIcon from 'material-ui/svg-icons/alert/warning'
import Checkbox from 'material-ui/Checkbox'
import Dialog from 'material-ui/Dialog'
import FlatButton from 'material-ui/FlatButton'
import Flexbox from 'flexbox-react'
import FloatingActionButton from 'material-ui/FloatingActionButton'
import IconButton from 'material-ui/IconButton'
import MenuItem from 'material-ui/MenuItem'
import Paper from 'material-ui/Paper'
import RaisedButton from 'material-ui/RaisedButton'
import SelectField from 'material-ui/SelectField'
import Snackbar from 'material-ui/Snackbar'
import TextField from 'material-ui/TextField'
import muiThemeable from 'material-ui/styles/muiThemeable'
import { List, ListItem } from 'material-ui/List'
import SortUp from 'material-ui/svg-icons/navigation/arrow-upward'
import SortDown from 'material-ui/svg-icons/navigation/arrow-downward'

import Mount from 'react-mount'

import AceEditor from 'react-ace'
import 'brace/mode/json'
import 'brace/theme/github'

import options from './options'
import {
  // actions
  set,
  fetch,
  update,
  save,
  validate,
  validateJson,
  validateStructure,
  reset,
  message,
  setInvalid,

  // selectors
  getProviders,
  isLoading,
  hasChanges,
  getMessage,
  getInvalid,
} from './reducer'

const submittingStyle = {
  position: 'absolute',
  top: 0,
  bottom: 0,
  right: 0,
  left: 0,
  background: 'rgba(0, 0, 0, 0.1)',
  zIndex: 9001,
  display: 'flex',
}

const Spinner = ({ submitting = false, children }) => (
  <div style={{ position: 'relative' }}>
    {submitting ? (
      <div style={submittingStyle}>
        <Flexbox justifyContent="center" alignItems="center" width="100%">
          <CircularProgress size={60} thickness={7} />
        </Flexbox>
      </div>
    ) : null}
    {children}
  </div>
)

const Title = muiThemeable()(({ children, muiTheme }) => (
  <h1 style={{ color: muiTheme.palette.textColor, textAlign: 'center' }}>
    {children}
  </h1>
))

const DeleteIconThemed = muiThemeable()(({ muiTheme }) => (
  <DeleteIcon style={{ color: muiTheme.palette.errorColor }} />
))

const Warning = muiThemeable()(({ children, muiTheme }) => (
  <Flexbox style={{ color: muiTheme.palette.warningColor }} alignItems="center">
    <div style={{ paddingRight: 10 }}>
      <WarningIcon color={muiTheme.palette.warningColor} />
    </div>
    <div>{children}</div>
  </Flexbox>
))

const implementationSupport = implementors => {
  if (!implementors.includes('ol')) {
    return '3D Only'
  } else if (!implementors.includes('cesium')) {
    return '2D Only'
  }
  return null
}

let Error = ({ muiTheme: { palette }, errorText, children }) => (
  <div>
    <div
      style={{
        border:
          '2px solid ' +
          (errorText !== undefined
            ? palette.errorColor
            : palette.disabledColor),
        borderRadius: 2,
      }}
    >
      {children}
    </div>
    <div style={{ color: palette.errorColor, marginTop: 8, fontSize: '0.8em' }}>
      {errorText}
    </div>
  </div>
)

Error = muiThemeable()(Error)

let Description = ({ muiTheme: { palette }, children }) => (
  <p style={{ color: palette.textColor }}>{children}</p>
)

Description = muiThemeable()(Description)

let Link = ({ muiTheme: { palette }, children, ...props }) => (
  <a style={{ color: palette.primary1Color }} {...props}>
    {children}
  </a>
)

Link = muiThemeable()(Link)

const bool = value => (typeof value === 'boolean' ? value : false)

const ProviderEditor = ({
  provider,
  onUpdate,
  buffer,
  onEdit,
  error = Map(),
}) => (
  <div style={{ padding: 16 }}>
    <div style={{ padding: '0 16px' }}>
      <TextField
        onChange={(e, value) => onUpdate(value, 'name')}
        fullWidth
        errorText={error.get('name')}
        value={provider.get('name') || ''}
        id="name"
        floatingLabelText="Name"
      />
    </div>
    <div style={{ padding: '0 16px' }}>
      <TextField
        onChange={(e, value) => onUpdate(value.replace(/\s/g, ''), 'url')}
        fullWidth
        errorText={error.get('url')}
        value={provider.get('url') || ''}
        id="provider-url"
        floatingLabelText="Provider URL"
      />
    </div>
    <div style={{ padding: '0 16px' }}>
      <Flexbox flex="1">
        <Checkbox
          label="Proxy Imagery Provider URL"
          checked={bool(provider.get('proxyEnabled'))}
          onCheck={(e, value) => {
            onUpdate(value, 'proxyEnabled')
          }}
        />
      </Flexbox>
    </div>
    <div style={{ padding: '0 16px' }}>
      <Flexbox flex="1">
        <Flexbox width="290px">
          <Checkbox
            label="Allow Credential Forwarding"
            checked={bool(provider.get('withCredentials'))}
            onCheck={(e, value) => {
              onUpdate(value, 'withCredentials')
            }}
          />
        </Flexbox>
        <Flexbox flex="1">
          {bool(provider.get('withCredentials')) ? (
            <Warning>
              Requests will fail if the server does not prompt for credentials
            </Warning>
          ) : null}
        </Flexbox>
      </Flexbox>
    </div>
    <Flexbox flex="1" style={{ padding: '0 16px' }}>
      <Flexbox flex="3" style={{ marginRight: 20 }}>
        <div style={{ width: '100%' }}>
          <SelectField
            floatingLabelText="Provider Type"
            fullWidth
            value={provider.get('type') || ''}
            errorText={error.get('type')}
            onChange={(e, i, value) => onUpdate(value, 'type')}
          >
            {Object.keys(options).map((type, i) => (
              <MenuItem
                key={i}
                value={type}
                primaryText={
                  <Flexbox flex="1" justifyContent="space-between">
                    <div>{options[type].label}</div>
                    <div>
                      {implementationSupport(Object.keys(options[type].help))}
                    </div>
                  </Flexbox>
                }
              />
            ))}
          </SelectField>
        </div>
      </Flexbox>
      <Flexbox flex="1">
        <TextField
          type="number"
          step={0.01}
          value={
            typeof provider.get('alpha') === 'number'
              ? provider.get('alpha')
              : ''
          }
          errorText={error.get('alpha')}
          floatingLabelText="Alpha (0 - 1)"
          fullWidth
          onChange={(e, value) => {
            if (value === '') {
              onUpdate('', 'alpha')
            } else {
              const n = Number(value)
              if (!(n < 0 || n > 1)) {
                onUpdate(n, 'alpha')
              }
            }
          }}
        />
      </Flexbox>
    </Flexbox>
    <div style={{ padding: '0 16px' }}>
      <Checkbox
        label="Show"
        id="show"
        checked={bool(provider.get('show'))}
        onCheck={(e, value) => {
          onUpdate(value, 'show')
        }}
      />
    </div>
    <div style={{ padding: '0 16px' }}>
      <Checkbox
        label="Transparent"
        id="transparent"
        checked={bool(provider.getIn(['parameters', 'transparent']))}
        labelStyle={{
          width: '200px',
        }}
        onCheck={(e, value) => {
          onUpdate(value, ['parameters', 'transparent'])
          if (value) {
            onUpdate('image/png', ['parameters', 'format'])
          } else {
            onUpdate('', ['parameters', 'format'])
          }
        }}
      />
    </div>
    <List>
      <ListItem
        primaryTogglesNestedList
        primaryText="Advanced Configuration"
        nestedItems={[
          <div key="description" style={{ margin: '0 15px' }}>
            <Description>
              Advanced provider configuration is specified in the{' '}
              <Link target="_blank" href="http://www.json.org">
                JSON
              </Link>{' '}
              format. Configuration properties can be found at the provided
              documentation links.
            </Description>
          </div>,
          options[provider.get('type')] !== undefined ? (
            <div
              key="docs"
              style={{
                textAlign: 'center',
                margin: '0 15px',
                marginBottom: 20,
              }}
            >
              <FlatButton
                primary
                target="_blank"
                label="openlayers docs"
                disabled={options[provider.get('type')].help.ol === undefined}
                href={options[provider.get('type')].help.ol}
              />
              <FlatButton
                primary
                target="_blank"
                label="cesium docs"
                disabled={
                  options[provider.get('type')].help.cesium === undefined
                }
                href={options[provider.get('type')].help.cesium}
              />
            </div>
          ) : null,
          <div key="ace" style={{ margin: '0 15px' }}>
            <Error
              errorText={
                ['buffer', 'proxyEnabled', 'order', 'show', 'withCredentials']
                  .map(key => error.get(key))
                  .filter(msg => msg !== undefined)[0]
              }
            >
              <AceEditor
                mode="json"
                theme="github"
                fontSize={15}
                tabSize={2}
                width="100%"
                height="400px"
                editorProps={{
                  $blockScrolling: Infinity,
                }}
                enableBasicAutocompletion
                name={provider.get('url')}
                value={buffer}
                onChange={onEdit}
              />
            </Error>
          </div>,
        ]}
      />
    </List>
  </div>
)

let FixedHeader = ({
  muiTheme: { palette },
  disabled = true,
  onSave,
  onReset,
}) => (
  <div
    style={{
      position: 'fixed',
      top: 0,
      right: 0,
      left: 0,
      zIndex: 100,
      background: palette.backdropColor,
    }}
  >
    <Flexbox
      flex="1"
      justifyContent="space-between"
      alignItems="center"
      style={{
        maxWidth: 960,
        margin: '0 auto',
        padding: '0 20px',
        borderBottom: `1px solid ${palette.disabledColor}`,
      }}
    >
      <Title>Map Layers Configuration</Title>
      <div>
        <RaisedButton
          disabled={disabled}
          primary
          label="save"
          onClick={onSave}
          style={{ marginRight: 10 }}
        />
        <FlatButton
          disabled={disabled}
          secondary
          label="reset"
          onClick={onReset}
        />
      </div>
    </Flexbox>
  </div>
)

FixedHeader = muiThemeable()(FixedHeader)

const FixConfig = ({ buffer, error, onUpdate, onDiscard, onSave }) => {
  const actions = [
    <FlatButton label="discard" secondary onClick={onDiscard} />,
    <RaisedButton
      label="keep"
      primary
      disabled={error !== undefined}
      keyboardFocused
      onClick={onSave}
    />,
  ]

  return (
    <Dialog
      title="Invalid Map Layer Configuration Found"
      actions={actions}
      open
    >
      <Description>
        Existing map layers configuration was found to be invalid. You can edit
        the JSON here and keep or discard the existing configuration.
      </Description>
      <Error errorText={error}>
        <AceEditor
          mode="json"
          theme="github"
          fontSize={15}
          tabSize={2}
          width="100%"
          height="400px"
          editorProps={{
            $blockScrolling: Infinity,
          }}
          enableBasicAutocompletion
          name="json-editor"
          value={buffer}
          onChange={onUpdate}
        />
      </Error>
    </Dialog>
  )
}

const UpArrow = ({ onUpdate }) => (
  <IconButton tooltip="Move Up" onClick={onUpdate}>
    <SortUp />
  </IconButton>
)

const DownArrow = ({ onUpdate }) => (
  <IconButton tooltip="Move Down" onClick={onUpdate}>
    <SortDown />
  </IconButton>
)

const MapLayers = props => {
  const {
    // actions
    onFetch,
    onUpdate,
    onSave,
    onReset,
    onMessage,
    onSetInvalid,
    onSet,

    // data
    disabled,
    providers = [],
    errors,
    loading,
    message,
    invalid,
  } = props

  return (
    <Spinner submitting={loading}>
      <Mount on={onFetch} />
      <FixedHeader onReset={onReset} onSave={onSave} disabled={disabled} />

      <div style={{ paddingTop: 96 }}>
        <Description>
          The following form allows users to configure imagery providers for{' '}
          <Link target="_blank" href="../../search/catalog">
            Intrigue
          </Link>
          .
        </Description>
        <Description>
          Some provider types are currently only supported by the 2D{' '}
          <Link target="_blank" href="https://openlayers.org">
            Openlayers
          </Link>{' '}
          map and some only by the 3D{' '}
          <Link target="_blank" href="https://cesiumjs.org">
            Cesium
          </Link>{' '}
          map.
        </Description>
        {providers.map((provider, i) => (
          <Paper key={i} style={{ position: 'relative', marginTop: 20 }}>
            {i === 0 ? (
              <div
                style={{
                  textAlign: 'center',
                  paddingTop: 20,
                  fontWeight: 'bold',
                  fontSize: '1.2rem',
                }}
              >
                Topmost Layer
              </div>
            ) : null}
            {i > 0 && i === providers.size - 1 ? (
              <div
                style={{
                  textAlign: 'center',
                  paddingTop: 20,
                  fontWeight: 'bold',
                  fontSize: '1.2rem',
                }}
              >
                Bottommost Layer
              </div>
            ) : null}
            <div style={{ position: 'absolute', top: 20, right: 20 }}>
              {i < providers.size - 1 ? (
                <DownArrow
                  onUpdate={() => {
                    onUpdate(i + 1, [i, 'layer', 'order'])
                    onUpdate(i, [i + 1, 'layer', 'order'])
                  }}
                />
              ) : null}
              {i > 0 ? (
                <UpArrow
                  onUpdate={() => {
                    onUpdate(i - 1, [i, 'layer', 'order'])
                    onUpdate(i, [i - 1, 'layer', 'order'])
                  }}
                />
              ) : null}
              <IconButton
                tooltip="Delete Layer"
                onClick={() => onUpdate(null, [i])}
              >
                <DeleteIconThemed />
              </IconButton>
            </div>
            <ProviderEditor
              error={errors.get(i)}
              provider={provider.get('layer')}
              onUpdate={(value, path = []) =>
                onUpdate(value, [i, 'layer'].concat(path))
              }
              buffer={provider.get('buffer')}
              onEdit={value => onUpdate(value, [i, 'buffer'])}
            />
          </Paper>
        ))}

        <Flexbox style={{ padding: 20 }} justifyContent="center">
          <FloatingActionButton
            onClick={() =>
              onUpdate(undefined, [providers.length || providers.size])
            }
          >
            <ContentAdd />
          </FloatingActionButton>
        </Flexbox>
        <Snackbar
          open={message.text !== undefined}
          message={message.text || ''}
          action={message.action}
          autoHideDuration={5000}
          onRequestClose={() => onMessage()}
          onActionTouchTap={() => window.open('../../search/catalog')}
        />
      </div>
      {invalid !== null ? (
        <FixConfig
          buffer={invalid}
          onSave={() => {
            const imageryProviders = JSON.parse(invalid)
            onSet(fromJS({ imageryProviders }))
            onSetInvalid(null)
          }}
          onDiscard={() => onSetInvalid(null)}
          onUpdate={buffer => onSetInvalid(buffer)}
          error={
            validateJson(invalid) || validateStructure(JSON.parse(invalid))
          }
        />
      ) : null}
    </Spinner>
  )
}

export default connect(
  state => {
    const providers = getProviders(state)
    const errors = validate(providers)
    const loading = isLoading(state)
    const disabled = !hasChanges(state)
    const message = getMessage(state)
    const invalid = getInvalid(state)
    return { providers, errors, loading, disabled, message, invalid }
  },
  {
    onFetch: fetch,
    onUpdate: update,
    onSave: save,
    onReset: reset,
    onMessage: message,
    onSetInvalid: setInvalid,
    onSet: set,
  }
)(MapLayers)
