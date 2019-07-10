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
import { hot } from 'react-hot-loader'

import styled from 'styled-components'

const Root = styled.div`
  position: fixed;
  top: 0;
  right: 0;
  z-index: 1001;
  height: 100vh;
  width: 480px;
  background: #253540;
  color: #ffffff;
  font-family: 'Open Sans', arial, sans-serif;
  border-left: 2px #34434c solid;
  line-height: 1.3em;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
`

const Type = styled.div`
  color: #32a6ad;
  font-weight: bold;
`

const Action = styled.div`
  border-bottom: 2px #34434c solid;
  display: flex;
  align-items: center;
  cursor: pointer;
`

const Icon = styled.div`
  padding: 20px;
  font-size: 2em;
  cursor: pointer;
  text-align: center;
`

const Input = styled.input`
  font-family: monospace;
  background: inherit;
  border: 2px #34434c solid;
  padding: 10px;
  width: 100%;
  box-sizing: border-box;
  color: #ffffff;
`

const TextArea = styled.textarea`
  font-size: 1.1em;
  font-family: monospace;
  background: inherit;
  border: 2px #34434c solid;
  padding: 10px;
  width: 100%;
  box-sizing: border-box;
  color: #ffffff;
`

const Description = styled.div`
  padding: 20px 0;
  width: 100%;
`

const Params = styled.div`
  padding: 10px;
  border-bottom: 2px #34434c solid;
`

const Param = styled.pre`
  cursor: pointer;
  margin: 0;
  padding: 0 10px;
`

const ActionParams = ({ action, onClick }) => {
  const params =
    typeof action.params === 'function' ? action.params() : action.params

  if (Array.isArray(params) && params.length > 0) {
    return (
      <Params>
        {params.map((param, i) => {
          const value = JSON.stringify(param)
          return (
            <Param
              key={i}
              onClick={() => {
                onClick(value)
              }}
            >
              {value}
            </Param>
          )
        })}
      </Params>
    )
  }

  return null
}

class ActionItem extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      open: false,
    }
  }
  render() {
    const { action, onRun, onParam } = this.props

    return (
      <div>
        <Action>
          <Icon onClick={onRun}>&#9654;</Icon>
          <Description>
            <Type>{action.type}</Type>
            <div>{action.docs || 'No Docs.'}</div>
          </Description>
        </Action>
        <ActionParams action={action} onClick={onParam} />
      </div>
    )
  }
}

const Button = styled.div`
  padding: 20px;
  box-sizing: border-box;
  width: 100%;
  border: 1px #34434c solid;
  text-align: center;
  background: ${props => (props.selected ? '#34434c' : '')};
  cursor: ${props => (props.selected ? 'initial' : 'pointer')};
`

const SelectorRoot = styled.div`
  display: flex;
  flex-direction: row;
`

const Selector = props => {
  const { value, onClick } = props
  return (
    <SelectorRoot>
      <Button selected={value === 'actions'} onClick={() => onClick('actions')}>
        Actions
      </Button>
      <Button selected={value === 'log'} onClick={() => onClick('log')}>
        Log
      </Button>
    </SelectorRoot>
  )
}

const copyToClipboard = str => {
  const el = document.createElement('textarea')
  el.value = str
  document.body.appendChild(el)
  el.select()
  document.execCommand('copy')
  document.body.removeChild(el)
}

const LogEntry = styled.div`
  border-bottom: 1px #34434c solid;
  padding: 10px;
`

const Log = ({ log, onClear }) => {
  return (
    <div>
      <SelectorRoot>
        <Button onClick={onClear}>Clear Log</Button>
        <Button
          onClick={() => {
            copyToClipboard(JSON.stringify(log, null, 2))
          }}
        >
          Export Log
        </Button>
      </SelectorRoot>
      {log.map(action => {
        return (
          <LogEntry>
            <Type>{action.type}</Type>
          </LogEntry>
        )
      })}
    </div>
  )
}

const Scrollable = styled.div`
  flex: 1;
  overflow-y: auto;
`

const Actions = props => {
  const actions = props.actions.map((action, i) => {
    return (
      <ActionItem
        key={i}
        action={action}
        onRun={() => props.onRun(action)}
        onParam={param => props.onParam(param)}
      />
    )
  })

  return <Scrollable>{actions}</Scrollable>
}

const FilterContainer = styled.div`
  border-bottom: 2px #34434c solid;
  border-top: 2px #34434c solid;
  padding: 10px;
  box-sizing: border-box;
`

class Monitor extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      open: true,
      filter: '',
      tab: 'actions',
      params: '',
      log: [],
    }
  }
  toggleOpen = event => {
    if (event.key === 'l' && event.ctrlKey) {
      event.stopPropagation()
      this.setState({ open: !this.state.open })
    }
  }
  componentDidMount() {
    this.poller = setInterval(this.onPoll, 250)
    document.addEventListener('keydown', this.toggleOpen)
  }
  componentWillUnmount() {
    clearInterval(this.poller)
    document.removeEventListener('keydown', this.toggleOpen)
  }
  onPoll = () => {
    this.setState({})
  }
  render() {
    const { api } = this.props

    if (!this.state.open) {
      return null
    }

    const { tab } = this.state

    const actions = api.getActions()

    return (
      <Root>
        <div style={{ display: 'flex', flexDirection: 'column', flex: 1 }}>
          {tab === 'actions' ? (
            <React.Fragment>
              <FilterContainer>
                <Input
                  value={this.state.filter}
                  onChange={e => {
                    this.setState({ filter: e.target.value })
                  }}
                  placeholder="type to filter actions..."
                />
              </FilterContainer>
              <Actions
                actions={actions.filter(({ type }) =>
                  type.toLowerCase().match(this.state.filter.toLowerCase())
                )}
                onRun={({ type }) => {
                  try {
                    const params =
                      this.state.params !== ''
                        ? JSON.parse(this.state.params)
                        : {}
                    const action = {
                      type,
                      ...params,
                    }
                    api.dispatch(action)
                    this.setState({
                      params: '',
                      log: this.state.log.concat(action),
                    })
                  } catch (e) {
                    console.error(e)
                  }
                }}
                onParam={params => {
                  this.setState({ params })
                }}
              />
            </React.Fragment>
          ) : (
            <Log
              log={this.state.log}
              onClear={() => this.setState({ log: [] })}
            />
          )}
        </div>
        <div>
          {tab === 'actions' ? (
            <FilterContainer>
              <TextArea
                rows="5"
                value={this.state.params}
                onChange={e => {
                  this.setState({ params: e.target.value })
                }}
                placeholder="action parameters"
              />
            </FilterContainer>
          ) : null}
          <Selector value={tab} onClick={tab => this.setState({ tab })} />
        </div>
      </Root>
    )
  }
}

export default hot(module)(Monitor)
