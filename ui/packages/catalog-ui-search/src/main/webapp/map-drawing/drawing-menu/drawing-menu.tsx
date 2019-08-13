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
import * as ol from 'openlayers'
import styled from 'styled-components'
import {
  Button,
  buttonTypeEnum,
} from '../../react-component/presentation/button'
import { Shape } from '../shape-utils'
import {
  BoundingBoxDrawingControl,
  DrawingContext,
  DrawingControl,
  LineDrawingControl,
  PointDrawingControl,
  PointRadiusDrawingControl,
  PolygonDrawingControl,
  UpdatedGeoReceiver,
} from '../drawing-controls'
import { transparentize, readableColor } from 'polished'
import { HTMLAttributes } from '../../react-component/html'
import BboxIcon from './icons/bbox'
import CircleIcon from './icons/circle'
import LineIcon from './icons/line'
import PolygonIcon from './icons/polygon'
import PointIcon from './icons/point'
import { GeometryJSON } from '../geometry'

type Props = HTMLAttributes & {
  shape: Shape | null
  map: ol.Map
  isActive: boolean
  showCoordinateEditor?: boolean
  saveAndContinue?: boolean
  title?: string
  geometry: GeometryJSON | null
  toggleCoordinateEditor?: () => void
  onCancel: () => void
  onOk: () => void
  onSetShape: (shape: string) => void
  onUpdate: UpdatedGeoReceiver
  disabledShapes?: Shape[]
  mapProjection: string
  userProjection: string
  mapStyle: ol.StyleFunction | ol.style.Style | ol.style.Style[]
}

type DrawingControlMap = Map<Shape, DrawingControl>

const InvisibleBackground = styled.div`
  display: none;
`
const TitleContainer = styled.div`
  display: flex;
  flex-direction: row;
  flex-grow: 1;
  height: 100%;
  font-size: ${props => props.theme.largeFontSize};
`
const TitleLabel = styled.div`
  color: ${props => readableColor(props.theme.positiveColor)};
  align-self: center;
  padding-left: ${props => props.theme.largeSpacing};
  padding-right: ${props => props.theme.minimumSpacing};
`
const Title = styled.div`
  color: ${props => readableColor(props.theme.positiveColor)};
  font-weight: bold;
  align-self: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`
const ShapeMenu = styled.div`
  height: 100%;
  display: flex;
  border-right: 1px solid ${props => props.theme.backgroundSlideout};
  border-left: 1px solid ${props => props.theme.backgroundSlideout};
  font-size: ${props => props.theme.minimumFontSize};
`
const ToolMenu = styled.div`
  height: 100%;
  display: flex;
  font-size: ${props => props.theme.minimumFontSize};
`
const StyledButton = styled(Button)`
  height: 100%;
  padding: 0px ${props => props.theme.largeSpacing};
  padding-top: 0;
  font-size: ${props => props.theme.largeFontSize};
  text-align: center;
  width: 5em;
`
const SelectableButton = styled.div<{ isSelected: boolean }>`
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: calc(${({ theme }) => theme.largeFontSize} * 1.5);
  padding: ${({ theme }) => theme.mediumSpacing};
  opacity: ${props => (props.isSelected ? 1 : props.theme.minimumOpacity)};
  cursor: pointer;
  color: ${props => readableColor(props.theme.positiveColor)};
  border: 1px solid
    ${props =>
      props.isSelected
        ? readableColor(props.theme.positiveColor)
        : 'transparent'};
  :hover {
    opacity: 1;
    border: 1px solid ${props => readableColor(props.theme.positiveColor)};
  }
`
const DrawingBackground = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
  height: 100%;
  width: 100%;
  max-width: 100%;
  background-color: ${props => transparentize(0.2, props.theme.positiveColor)};
`

class DrawingMenu extends React.Component<Props> {
  drawingContext: DrawingContext
  controlsMap: DrawingControlMap
  setShape: (shape: string) => void
  acceptEdit: (e: any) => void
  cancelClick: (e: any) => void

  constructor(props: Props) {
    super(props)
    this.drawingContext = new DrawingContext({
      map: this.props.map,
      drawingStyle: props.mapStyle,
      userProjection: props.userProjection,
      mapProjection: props.mapProjection,
    })
    this.controlsMap = new Map<Shape, DrawingControl>()
    this.controlsMap.set(
      'Polygon',
      new PolygonDrawingControl(this.drawingContext, this.props.onUpdate)
    )
    this.controlsMap.set(
      'Line',
      new LineDrawingControl(this.drawingContext, this.props.onUpdate)
    )
    this.controlsMap.set(
      'Point Radius',
      new PointRadiusDrawingControl(this.drawingContext, this.props.onUpdate)
    )
    this.controlsMap.set(
      'Point',
      new PointDrawingControl(this.drawingContext, this.props.onUpdate)
    )
    this.controlsMap.set(
      'Bounding Box',
      new BoundingBoxDrawingControl(this.drawingContext, this.props.onUpdate)
    )
    this.setShape = (shape: string) => {
      this.props.onSetShape(shape)
    }
    this.acceptEdit = (_e: any) => {
      this.cancelShapeDrawing()
      this.props.onOk()
    }
    this.cancelClick = (_e: any) => {
      this.cancelShapeDrawing()
      this.props.onCancel()
    }
  }

  drawShape() {
    if (
      this.props.isActive &&
      this.props.shape !== null &&
      this.props.geometry !== null
    ) {
      this.cancelShapeDrawing()
      const control = this.controlsMap.get(this.props.shape)
      if (control !== undefined) {
        control.startDrawing(this.props.geometry)
      }
    }
  }

  cancelShapeDrawing() {
    this.controlsMap.forEach((control: DrawingControl, _shape: Shape) => {
      control.cancelDrawing()
    })
  }

  setDrawingActive(active: boolean) {
    const control = this.controlsMap.get(this.props.shape)
    if (control !== undefined) {
      control.setActive(active)
    }
  }

  isDrawing(): boolean {
    const control = this.controlsMap.get(this.props.shape)
    if (control !== undefined) {
      return control.isDrawing()
    }
    return false
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.shape !== this.props.shape) {
      this.cancelShapeDrawing()
      this.drawShape()
    } else if (prevProps.isActive !== this.props.isActive) {
      if (this.props.isActive) {
        this.drawShape()
      } else {
        this.cancelShapeDrawing()
      }
    }
    if (this.isDrawing()) {
      this.setDrawingActive(this.props.showCoordinateEditor !== true)
    }
    if (
      this.props.showCoordinateEditor != prevProps.showCoordinateEditor &&
      this.props.showCoordinateEditor === false
    ) {
      this.cancelShapeDrawing()
      this.drawShape()
      // Note: If we refactor drawing controls to separate start drawing from update feature
      // it may be possible to update the geometry features live.
    }
  }

  renderShapeButton(shape: Shape, icon: any) {
    return this.props.disabledShapes &&
      this.props.disabledShapes.includes(shape) ? null : (
      <SelectableButton
        isSelected={this.props.shape === shape}
        onClick={() => this.setShape(shape)}
        title={`Draw ${shape}`}
      >
        {icon}
      </SelectableButton>
    )
  }

  render() {
    const {
      shape,
      map,
      isActive,
      geometry,
      onCancel,
      onOk,
      onSetShape,
      onUpdate,
      title,
      saveAndContinue,
      showCoordinateEditor,
      toggleCoordinateEditor,
      ...rest
    } = this.props
    const Background = isActive ? DrawingBackground : InvisibleBackground
    const acceptEditButton = saveAndContinue ? 'Next' : 'Apply'
    const acceptEditAlt = saveAndContinue
      ? 'Save And Continue Drawing'
      : 'Accept Edit'
    return (
      <Background {...rest}>
        <TitleContainer>
          {title === undefined ? null : (
            <React.Fragment>
              <TitleLabel>Editing Shape:</TitleLabel>
              <Title>{title}</Title>
            </React.Fragment>
          )}
        </TitleContainer>
        <ShapeMenu>
          {this.renderShapeButton('Line', <LineIcon />)}
          {this.renderShapeButton('Polygon', <PolygonIcon />)}
          {this.renderShapeButton('Bounding Box', <BboxIcon />)}
          {this.renderShapeButton('Point Radius', <CircleIcon />)}
          {this.renderShapeButton('Point', <PointIcon />)}
        </ShapeMenu>
        {showCoordinateEditor === undefined ||
        toggleCoordinateEditor === undefined ? null : (
          <ToolMenu>
            <SelectableButton
              isSelected={showCoordinateEditor}
              onClick={toggleCoordinateEditor}
              title="Edit Coordinates"
            >
              <span className="fa fa-th-list" />
            </SelectableButton>
          </ToolMenu>
        )}
        <StyledButton
          buttonType={buttonTypeEnum.positive}
          type="button"
          onClick={this.cancelClick}
          title="Cancel Edit"
        >
          Cancel
        </StyledButton>
        <StyledButton
          buttonType={buttonTypeEnum.primary}
          type="button"
          onClick={this.acceptEdit}
          title={acceptEditAlt}
        >
          {acceptEditButton}
        </StyledButton>
      </Background>
    )
  }
}

export default DrawingMenu
