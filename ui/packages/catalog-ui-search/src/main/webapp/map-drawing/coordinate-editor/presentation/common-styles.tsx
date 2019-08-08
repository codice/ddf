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
import styled from 'styled-components'
import ToggleButton from './toggle-button'

const BBoxRoot = styled.div<{ flexDirection: 'column' | 'row' }>`
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: ${props => props.flexDirection};
  min-width: 25rem;
  min-height: 9.5rem;
`
const Column = styled.div`
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
`
const Row = styled.div`
  margin: 0;
  padding: 0;
  display: flex;
`
const Label = styled.div`
  display: flex;
  margin: 0;
  padding: 0;
  justify-content: flex-end;
  align-items: center;
  width: 6em;
  margin-right: ${props => props.theme.minimumSpacing};
  font-size: ${({ theme }) => theme.minimumFontSize};
`
const CompactLabel = styled(Label)`
  width: 4em;
`
const SpacedToggleButton = styled(ToggleButton)`
  margin-right: ${props => props.theme.minimumSpacing};
  font-size: ${({ theme }) => theme.minimumFontSize};
`
const SpacedInputLabelRow = styled.label`
  margin: 0;
  padding: 0;
  display: flex;
  align-items: center;
  margin-bottom: ${props => props.theme.minimumSpacing};
`

export {
  Column,
  Row,
  Label,
  CompactLabel,
  SpacedToggleButton,
  SpacedInputLabelRow,
  BBoxRoot,
}
