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
import * as React from 'react'
import styled from '@connexta/atlas/styled'
import { hot } from 'react-hot-loader'
import ServiceElement, { ServiceType } from '../../container/service'

type Props = {
  services: ServiceType[]
}

const Header = styled.h4`
  text-align: center;
`

const render = (props: Props) => {
  const { services } = props
  return (
    <>
      {services.length === 0 ? (
        <Header>
          <span className="fa fa-refresh fa-spin fa-5x" />
          <div>Loading Configurations</div>
        </Header>
      ) : (
        services.map((service: ServiceType) => {
          return <ServiceElement key={service.id} {...service} />
        })
      )}
    </>
  )
}

export default hot(module)(render)
