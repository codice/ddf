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
import styled from '../../styles/styled-components'
import UnsavedIndicator from '../unsaved-indicator'
const SlideoutLeftViewInstance = require('../../../component/singletons/slideout.left.view-instance.js')
import ExtensionPoints from '../../../extension-points'
import { Button, buttonTypeEnum } from '../button'
const HandlebarsHelpers = require('../../../js/HandlebarsHelpers')
import { hot } from 'react-hot-loader'

const StyledUnsavedIndicator = styled(UnsavedIndicator)`
  position: absolute;
  left: ${props => {
    return props.theme.minimumButtonSize
  }};
  top: -0.3125rem;
`

export interface Props {
  hasUnsaved: boolean
  hasUnavailable: boolean
  hasLogo: boolean
  logo: string
}

const Root = styled<Props, 'div'>('div')`
  position: relative;
  overflow: hidden;
  cursor: pointer;
  width: ${props => {
    if (props.hasLogo) {
      if (props.hasUnavailable || props.hasUnsaved) {
        return `calc(2*${props.theme.minimumButtonSize} + 1rem)`
      } else {
        return props.theme.multiple(2, props.theme.minimumButtonSize)
      }
    } else if (props.hasUnavailable || props.hasUnsaved) {
      return `calc(${props.theme.minimumButtonSize} + 1rem)`
    } else {
      return props.theme.minimumButtonSize
    }
  }};
  transition: width ${props => props.theme.coreTransitionTime} ease-out;

  button {
    text-align: left;
    height: 100%;
    width: 100%;
    padding-right: 0rem;
    line-height: inherit;
  }

  button > .fa-bars {
    display: inline-block;
    width: ${props => props.theme.minimumButtonSize};
    height: 100%;
    text-align: center;
  }

  .navigation-multiple,
  .navigation-sources {
    opacity: 0;
    transform: scale(2);
    transition: ${props => {
      return `transform ${props.theme.coreTransitionTime} ease-out, opacity ${
        props.theme.coreTransitionTime
      } ease-out;`
    }};
  }

  button > .navigation-sources {
    position: absolute;
    left: ${props => props.theme.minimumButtonSize};
    color: ${props => props.theme.warningColor};
  }

  button > .navigation-multiple {
    position: absolute;
    left: ${props => props.theme.minimumButtonSize};
    color: ${props => props.theme.warningColor};
  }

  &.has-logo {
    img {
      max-width: ${props => props.theme.minimumButtonSize};
      max-height: ${props => props.theme.minimumButtonSize};
      vertical-align: top;
      position: absolute;
      top: 50%;
      transform: translateY(-50%) translateX(0);
      transition: ${props => {
        return `transform ${props.theme.coreTransitionTime} ease-out;`
      }};
    }

    &.has-unavailable,
    &.has-unsaved {
      img {
        transform: translateY(-50%) translateX(1rem);
      }
    }
  }

  &.has-unavailable {
    .navigation-sources {
      opacity: 1;
      transform: scale(1);
    }
  }

  &.has-unavailable.has-unsaved {
    .navigation-sources {
      opacity: 0;
      transform: scale(2);
    }

    .navigation-multiple {
      opacity: 1;
      transform: scale(1);
    }
  }
`

const handleUnsaved = (props: Props, classes: string[]) => {
  if (props.hasUnsaved) {
    classes.push('has-unsaved')
  }
}

const handleUnavailable = (props: Props, classes: string[]) => {
  if (props.hasUnavailable) {
    classes.push('has-unavailable')
  }
}

const handleLogo = (props: Props, classes: string[]) => {
  if (props.hasLogo) {
    classes.push('has-logo')
  }
}

const getClassesFromProps = (props: Props) => {
  const classes: string[] = []
  handleUnsaved(props, classes)
  handleUnavailable(props, classes)
  handleLogo(props, classes)
  return classes.join(' ')
}

const openNavigator = () => {
  SlideoutLeftViewInstance.updateContent(() => {
    return (
      <ExtensionPoints.navigator
        closeSlideout={() => {
          SlideoutLeftViewInstance.close()
        }}
      />
    )
  })
  SlideoutLeftViewInstance.open()
}

function NavigationLeft(props: Props) {
  return (
    <Root
      className={`${getClassesFromProps(props)}`}
      {...props}
      onClick={() => openNavigator()}
    >
      <Button
        title="Click here to bring up the navigator"
        buttonType={buttonTypeEnum.neutral}
        fadeUntilHover={true}
      >
        <span className="fa fa-bars" />
        <StyledUnsavedIndicator
          shown={props.hasUnsaved && !props.hasUnavailable}
        />
        <span className="navigation-sources fa fa-bolt" />
        <span className="navigation-multiple fa fa-exclamation" />
        {props.hasLogo ? (
          <img
            className="logo"
            src={HandlebarsHelpers.getImageSrc(props.logo)}
          />
        ) : null}
      </Button>
    </Root>
  )
}

export default hot(module)(NavigationLeft)
