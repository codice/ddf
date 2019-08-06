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

import { hot } from 'react-hot-loader'
import * as React from 'react'
import styled from '../../styles/styled-components'
import LoadingCompanion from '../../container/loading-companion'

type Props = {
  metacardValidation: any
  attributeValidation: any
  loading: boolean
}

const Header = styled.h4`
  text-align: left;
  padding: ${props => props.theme.minimumSpacing};
`

const Root = styled.div`
  overflow: auto;

  table {
    width: 100%;
    text-align: center;
    margin-bottom: 40px;
  }

  th {
    width: 33%;
    text-align: center;
  }

  tr:nth-of-type(even) {
    background: rgba(0, 0, 0, 0.1);
  }

  th,
  td {
    padding: 10px;
  }

  td + td {
    border-left: 1px solid rgba(100, 100, 100, 0.3);
  }

  tbody {
    border-top: 1px solid rgba(100, 100, 100, 0.3);
  }
`

const MetacardValidation = (props: any) => {
  const metacardValidation = props.metacardValidation
  return (
    <>
      <Header>Metacard Validation Issues</Header>
      <table>
        <thead>
          <th>Attribute</th>
          <th>Severity</th>
          <th>Message</th>
        </thead>
        <tbody>
          {metacardValidation.map((validation: any, i: number) => {
            return (
              <tr key={i}>
                <td>
                  {validation.attributes.map((attribute: string, j: number) => {
                    return <div key={attribute + j}>{attribute}</div>
                  })}
                </td>
                <td>{validation.severity}</td>
                {validation.duplicate ? (
                  <td>
                    {validation.duplicate.message[0]}
                    {validation.duplicate.ids.map((id: any, index: number) => {
                      return (
                        <React.Fragment key={id}>
                          <a href={`#metacards/${id}`}>{id}</a>
                          {index !== validation.duplicate.ids.length - 1
                            ? ', '
                            : ''}
                        </React.Fragment>
                      )
                    })}
                    {validation.duplicate.message[1]}
                  </td>
                ) : (
                  <td>{validation.message}</td>
                )}
              </tr>
            )
          })}
        </tbody>
      </table>
    </>
  )
}

const AttributeValidation = (props: any) => {
  const attributeValidation = props.attributeValidation
  return (
    <>
      <Header>Attribute Validation Issues</Header>
      <table>
        <thead>
          <th>Attribute</th>
          <th>Warnings</th>
          <th>Errors</th>
        </thead>
        <tbody>
          {attributeValidation.map((validation: any, i: number) => {
            return (
              <tr key={i}>
                <td>{validation.attribute}</td>
                <td>
                  {validation.warnings.map((warning: string, j: number) => {
                    return <div key={warning + j}>{warning}</div>
                  })}
                </td>
                <td>
                  {validation.errors.map((error: string, j: number) => {
                    return <div key={error + j}>{error}</div>
                  })}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </>
  )
}

const render = (props: Props) => {
  const { metacardValidation, attributeValidation, loading } = props
  return (
    <LoadingCompanion loading={loading}>
      <Root>
        {metacardValidation.length > 0 ? (
          <MetacardValidation metacardValidation={metacardValidation} />
        ) : (
          <Header>No Metacard Validation Issues to Report</Header>
        )}

        {attributeValidation.length > 0 ? (
          <AttributeValidation attributeValidation={attributeValidation} />
        ) : (
          <Header>No Attribute Validation Issues to Report</Header>
        )}
      </Root>
    </LoadingCompanion>
  )
}

export default hot(module)(render)
