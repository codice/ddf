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
import { connect } from 'react-redux'
import { Card, CardMedia, CardTitle, CardText } from 'material-ui/Card'
import CircularProgress from 'material-ui/CircularProgress'
import FlatButton from 'material-ui/FlatButton'
import Flexbox from 'flexbox-react'
import RaisedButton from 'material-ui/RaisedButton'
import Snackbar from 'material-ui/Snackbar'
import muiThemeable from 'material-ui/styles/muiThemeable'
import { Toolbar, ToolbarGroup, ToolbarTitle } from 'material-ui/Toolbar'
import Mount from 'react-mount'
import AceEditor from 'react-ace'
import 'brace/mode/json'
import 'brace/theme/github'

import {
  // actions
  fetch,
  update,
  save,
  rendered,
  validate,
  reset,
  message,

  // selectors
  getBuffer,
  isLoading,
  hasChanges,
  getMessage,
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
      <Title>Default Layout Configuration</Title>
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

const LayoutEditor = ({ onRender }) => (
  <Mount did={onRender}>
    <div id="layoutContainer" style={{ height: 650 }} />
  </Mount>
)

const LayoutMenu = () => (
  <Toolbar>
    <ToolbarGroup firstChild />
    <ToolbarGroup>
      <ToolbarTitle text="Visualizations" />
      <div id="layoutMenu" />
    </ToolbarGroup>
  </Toolbar>
)

const ConfigEditor = ({ buffer, onEdit, error }) => (
  <div style={{ padding: 16 }}>
    <div
      key="docs"
      style={{ textAlign: 'center', margin: '0 15px', marginBottom: 20 }}
    >
      <FlatButton
        primary
        target="_blank"
        label="Window Items"
        disabled={false}
        href={'http://golden-layout.com/docs/ItemConfig.html'}
      />
    </div>
    <div key="ace" style={{ margin: '0 15px' }}>
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
          name={'layoutConfig'}
          value={buffer}
          onChange={onEdit}
        />
      </Error>
    </div>
  </div>
)

const MapLayers = ({
  onFetch,
  onRender,
  onUpdate,
  onSave,
  onAdd,
  onReset,
  onMessage,
  disabled,
  buffer,
  error,
  loading,
  message,
}) => (
  <Spinner submitting={loading}>
    <Mount on={onFetch} />
    <FixedHeader onReset={onReset} onSave={onSave} disabled={disabled} />
    <div style={{ paddingTop: 96 }}>
      <Description>
        The following form allows administrators to configure the default layout
        for visualization windows within{' '}
        <Link target="_blank" href="../../search/catalog">
          Intrigue
        </Link>
        .
      </Description>
      <Card>
        <LayoutMenu />
        <CardMedia>
          <LayoutEditor onRender={onRender} />
        </CardMedia>
      </Card>
      <Card style={{ position: 'relative', marginTop: 20 }}>
        <CardTitle title="Advanced Generated Configuration" />
        <CardText>
          This is the automatically generated configuration based on the layout
          specified in the layout editor above. The advanced default window
          layout configuration is specified in the{' '}
          <Link target="_blank" href="http://www.json.org">
            JSON
          </Link>{' '}
          format. A description of the configuration properties for the
          visualization windows within the default layout can be found at the
          provided documentation link. NOTE: If the JSON is malformed or has
          invalid components, the visualization window will not update and
          changes will not be saved.
        </CardText>
        <ConfigEditor
          error={error}
          buffer={buffer.get('buffer')}
          onEdit={value => onUpdate(value, 'buffer')}
        />
      </Card>
      <Snackbar
        open={message.text !== undefined}
        message={message.text || ''}
        action={message.action}
        autoHideDuration={5000}
        onRequestClose={() => onMessage()}
        onActionTouchTap={() => window.open('../../search/catalog')}
      />
    </div>
  </Spinner>
)

export default connect(
  state => {
    const buffer = getBuffer(state)
    const error = validate(buffer)
    const loading = isLoading(state)
    const disabled = !hasChanges(state)
    const message = getMessage(state)
    return { buffer, error, loading, disabled, message }
  },
  {
    onFetch: fetch,
    onRender: rendered,
    onUpdate: update,
    onSave: save,
    onReset: reset,
    onMessage: message,
  }
)(MapLayers)
