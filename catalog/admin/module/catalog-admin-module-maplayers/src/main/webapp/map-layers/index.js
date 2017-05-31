import React from 'react'

import { Map } from 'immutable'
import { connect } from 'react-redux'

import CircularProgress from 'material-ui/CircularProgress'
import ContentAdd from 'material-ui/svg-icons/content/add'
import DeleteIcon from 'material-ui/svg-icons/action/delete'
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

import Mount from 'react-mount'

import AceEditor from 'react-ace'
import 'brace/mode/json'
import 'brace/theme/github'

import options from './options'
import {
  // actions
  fetch,
  update,
  save,
  validate,
  reset,
  message,

  // selectors
  getProviders,
  isLoading,
  hasChanges,
  getMessage
} from './reducer'

const submittingStyle = {
  position: 'absolute',
  top: 0,
  bottom: 0,
  right: 0,
  left: 0,
  background: 'rgba(0, 0, 0, 0.1)',
  zIndex: 9001,
  display: 'flex'
}

const Spinner = ({ submitting = false, children }) => (
  <div style={{ position: 'relative' }}>
    {submitting
      ? <div style={submittingStyle}>
        <Flexbox justifyContent='center' alignItems='center' width='100%'>
          <CircularProgress size={60} thickness={7} />
        </Flexbox>
      </div>
      : null}
    {children}
  </div>
)

const Title = muiThemeable()(({ children, muiTheme }) => (
  <h1 style={{ color: muiTheme.palette.textColor, textAlign: 'center' }}>{children}</h1>
))

const implementationSupport = (implementors) => {
  if (!implementors.includes('ol')) {
    return '3D Only'
  } else if (!implementors.includes('cesium')) {
    return '2D Only'
  }
  return null
}

let Error = ({ muiTheme: { palette }, errorText, children }) => (
  <div>
    <div style={{
      border: ('2px solid ' + (errorText !== undefined ? palette.errorColor : palette.disabledColor)),
      borderRadius: 2
    }}>
      {children}
    </div>
    <div style={{ color: palette.errorColor, marginTop: 8, fontSize: '0.8em' }}>{errorText}</div>
  </div>
)

Error = muiThemeable()(Error)

let Description = ({ muiTheme: { palette }, children }) => (
  <p style={{ color: palette.textColor }}>{children}</p>
)

Description = muiThemeable()(Description)

let Link = ({ muiTheme: { palette }, children, ...props }) => (
  <a style={{ color: palette.primary1Color }} {...props}>{children}</a>
)

Link = muiThemeable()(Link)

const ProviderEditor = ({ provider, onUpdate, buffer, onEdit, error = Map() }) => (
  <div style={{ padding: 16 }}>
    <div style={{ padding: '0 16px' }}>
      <TextField
        onChange={(e, value) => onUpdate(value.replace(/\s/g, ''), 'url')}
        fullWidth
        errorText={error.get('url')}
        value={provider.get('url') || ''}
        id='provider-url'
        floatingLabelText='Provider URL' />
    </div>
    <Flexbox flex='1' style={{ padding: '0 16px' }}>
      <Flexbox flex='5' style={{ marginRight: 20 }}>
        <div style={{ width: '100%' }}>
          <SelectField
            floatingLabelText='Provider Type'
            fullWidth
            value={provider.get('type') || ''}
            errorText={error.get('type')}
            onChange={(e, i, value) => onUpdate(value, 'type')}>
            {Object.keys(options).map((type, i) =>
              <MenuItem
                key={i}
                value={type}
                primaryText={
                  <Flexbox flex='1' justifyContent='space-between'>
                    <div>{options[type].label}</div>
                    <div>
                      {implementationSupport(Object.keys(options[type].help))}
                    </div>
                  </Flexbox>} />)}
          </SelectField>
        </div>
      </Flexbox>
      <Flexbox flex='1'>
        <TextField
          type='number'
          step={0.01}
          value={typeof provider.get('alpha') === 'number' ? provider.get('alpha') : ''}
          errorText={error.get('alpha')}
          floatingLabelText='Alpha'
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
          }} />
      </Flexbox>
    </Flexbox>
    <List>
      <ListItem
        primaryTogglesNestedList
        primaryText='Advanced Configuration'
        nestedItems={[
          <div key='description' style={{ margin: '0 15px' }}>
            <Description>
              Advanced provider configuration is specified in
              the <Link target='_blank' href='http://www.json.org'>JSON</Link> format.
              Configuration properties can be found at the provided documentation links.
            </Description>
          </div>,
          (options[provider.get('type')] !== undefined)
            ? <div key='docs' style={{ textAlign: 'center', margin: '0 15px', marginBottom: 20 }}>
              <FlatButton
                primary
                target='_blank'
                label='openlayers docs'
                disabled={options[provider.get('type')].help.ol === undefined}
                href={options[provider.get('type')].help.ol} />
              <FlatButton
                primary
                target='_blank'
                label='cesium docs'
                disabled={options[provider.get('type')].help.cesium === undefined}
                href={options[provider.get('type')].help.cesium} />
            </div> : null,
          <div key='ace' style={{ margin: '0 15px' }}>
            <Error errorText={error.get('buffer')}>
              <AceEditor
                mode='json'
                theme='github'
                fontSize={15}
                tabSize={2}
                width='100%'
                height='400px'
                editorProps={{
                  $blockScrolling: Infinity
                }}
                enableBasicAutocompletion
                name={provider.get('url')}
                value={buffer}
                onChange={onEdit} />
            </Error>
          </div> ]} />
    </List>
  </div>
)

let FixedHeader = ({ muiTheme: { palette }, disabled = true, onSave, onReset }) => (
  <div style={{
    position: 'fixed',
    top: 0,
    right: 0,
    left: 0,
    zIndex: 100,
    background: palette.backdropColor
  }}>
    <Flexbox
      flex='1'
      justifyContent='space-between'
      alignItems='center'
      style={{
        maxWidth: 960,
        margin: '0 auto',
        padding: '0 20px',
        borderBottom: `1px solid ${palette.disabledColor}` }}>
      <Title>Map Layers Configuration</Title>
      <div>
        <RaisedButton disabled={disabled} primary label='save' onClick={onSave} style={{ marginRight: 10 }} />
        <FlatButton disabled={disabled} secondary label='reset' onClick={onReset} />
      </div>
    </Flexbox>
  </div>
)

FixedHeader = muiThemeable()(FixedHeader)

const MapLayers = ({ onFetch, onUpdate, onSave, onReset, onMessage, disabled, providers = [], errors, loading, message }) => (
  <Spinner submitting={loading}>
    <Mount on={onFetch} />
    <FixedHeader onReset={onReset} onSave={onSave} disabled={disabled} />

    <div style={{ paddingTop: 96 }}>
      <Description>
        The following form allows users to configure imagery providers
        for <Link target='_blank' href='/search/catalog'>Intrigue</Link>.
        Providers are sorted by alpha, where higher alpha providers appear below
        lower alpha providers on the map.
      </Description>
      <Description>
        Some provider types are currently only support by the 2D <Link target='_blank'
          href='https://openlayers.org'>Openlayers</Link> map and some only
        by the 3D <Link target='_blank' href='https://cesiumjs.org'>Cesium</Link> map.
      </Description>
      {providers.map((provider, i) =>
        <Paper key={i} style={{ position: 'relative', marginTop: 20 }}>
          <IconButton
            tooltip='Delete Layer'
            style={{ position: 'absolute', top: 20, right: 20 }}
            onClick={() => onUpdate(null, [i])}>
            <DeleteIcon />
          </IconButton>
          <ProviderEditor
            error={errors.get(i)}
            provider={provider.get('layer')}
            onUpdate={(value, path = []) => onUpdate(value, [i, 'layer'].concat(path))}
            buffer={provider.get('buffer')}
            onEdit={(value) => onUpdate(value, [i, 'buffer'])} />
        </Paper>)}

      <Flexbox style={{ padding: 20 }} justifyContent='center'>
        <FloatingActionButton onClick={() => onUpdate(undefined, [providers.length || providers.size])}>
          <ContentAdd />
        </FloatingActionButton>
      </Flexbox>
      <Snackbar
        open={message.text !== undefined}
        message={message.text || ''}
        action={message.action}
        autoHideDuration={5000}
        onRequestClose={() => onMessage()}
        onActionTouchTap={() => window.open('/search/catalog')} />
    </div>
  </Spinner>
)

export default connect(
  (state) => {
    const providers = getProviders(state)
    const errors = validate(providers)
    const loading = isLoading(state)
    const disabled = !hasChanges(state)
    const message = getMessage(state)
    return { providers, errors, loading, disabled, message }
  },
  { onFetch: fetch, onUpdate: update, onSave: save, onReset: reset, onMessage: message }
)(MapLayers)
