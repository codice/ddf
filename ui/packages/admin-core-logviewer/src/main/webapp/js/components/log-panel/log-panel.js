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

import LogViewer from '../log-viewer/log-viewer'
import styled from '@connexta/atlas/styled'

const Root = styled.div`
  .text-filter {
    width: 100%;
    display: inline-block;
    margin: 0px;
    padding: 5px;
    box-sizing: border-box;
    border-radius: 3px;
    border: 1px #ccc solid;
    outline: 0;
    line-height: 18px;
  }
  .button-base {
    width: 100%;
    height: 30px;
    color: #fff;
    font-size: 16px;
    font-family: Helvetica;
    background-color: #18bc9c;
    border-color: #18bc9c;
    border: 1px solid transparent;
    white-space: nowrap;
    text-align: center;
    border-radius: 4px;
    outline: 0;
  }

  .button-live {
    .button-base;
    color: #fff;
    background-color: #e74c3c;
    border-color: #e74c3c;
  }

  .button-paused {
    .button-base;
    color: #666;
    background-color: #eee;
    border-color: #666;
  }

  .status-text {
    min-width: 80px;
    display: inline-block;
    text-align: center;
  }

  .container {
    position: relative;
    height: 100%;
    background: #ccc;
    max-width: 100%;
    padding: 0px;
  }

  .filterRow {
    color: #2c3e50;
    background: #fff;
    width: 100%;
    height: 60px;
    border-bottom: 1px #ccc solid;
    position: relative;
    z-index: 1;
    box-shadow: 0 0 3px 0px rgba(0, 0, 0, 0.3);

    select {
      width: 100%;
      position: relative;
      top: 4px;
    }
  }

  .logRows {
    overflow-y: auto;
    overflow-x: hidden;
  }

  .table {
    border-collapse: collapse;
    table-layout: fixed;
    width: 100%;
  }

  .header {
    color: #2c3e50;
    background: #ecf0f1;
    padding: 2px;
  }

  .logs {
    display: table-row;
    position: relative;
  }

  .controls {
    padding: 4px;
  }

  .loading {
    color: #2c3e50;
    padding: 10px;
    font-size: 24px;
    text-align: center;
  }

  .dimUnselected tr:not(.selectedRow) {
    opacity: 0.6;
  }

  .error-message {
    background: #e74c3c;
    padding: 10px 20px;
    width: 100%;
    box-sizing: border-box;
    cursor: pointer;
    color: white;
    overflow: hidden;
  }

  .float-right {
    float: right;
  }

  .panel {
    position: relative;
    font-family: sans-serif;
    font-size: 14px;
  }

  .newTabLink {
    color: #18bc9c;
    text-align: right;
    float: right;
    font-family: sans-serif;
  }

  @keyframes slideAnimationKeyframes {
    0% {
      opacity: 0;
      transform: translateY(50px);
    }

    100% {
      opacity: 1;
      transform: translateX(0);
    }
  }

  .rowData {
    border-bottom: 1px #ccc solid;
    padding: 5px;
    word-wrap: break-word;
    cursor: pointer;
  }

  .selectedRow {
    opacity: 1;
  }

  .dim {
    opacity: 0.5;
  }

  .rowAnimation {
    position: relative;
    animation: slideAnimationKeyframes ease 0.25s;
  }

  .timestampColumn {
    width: 175px;
  }

  .levelColumn {
    width: 90px;
  }

  .message {
    text-overflow: ellipsis;
    width: 100%;
    overflow: hidden;
    white-space: nowrap;
    background-color: inherit;
  }

  .messageExpanded {
    white-space: normal;
  }

  .bundleColumn {
    width: 200px;
  }

  .errorLevel {
    background: #ffe6e6;
  }

  .warnLevel {
    background: #ffffe6;
  }

  .infoLevel {
    background: white;
  }

  .debugLevel {
    background: #e6ffe6;
  }

  .traceLevel {
    background: #e6e6ff;
  }
`

const panelClass = () => {
  if (window === window.top) {
    return 'panel'
  } else {
    return 'panel-iframe'
  }
}

const LogPanel = connect(({ filter, logs, displaySize, expandedHash }) => ({
  filter,
  logs,
  displaySize,
  expandedHash,
}))(LogViewer)

export default () => {
  return (
    <Root>
      <div className={panelClass()}>
        <LogPanel />
      </div>
    </Root>
  )
}
