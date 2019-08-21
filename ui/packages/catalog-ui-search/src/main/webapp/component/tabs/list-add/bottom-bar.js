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
import styled from 'styled-components'

const BottomBarContainer = styled.div`
    position: absolute;
    width: 100%;
    bottom: 0;
    display: flex;
    flex-flow: row-reverse;
    border-top: ${props => props.theme.backgroundNavigation} 2px solid;
    padding: ${props => props.theme.minimumSpacing};
`

const ButtonStyle = styled.button`
    padding: 0px ${props => props.theme.minimumSpacing};
    min-width: ${props => props.theme.minimumButtonSize};
`

const BottomMessageContainer = styled.div`
    position: absolute;
    padding: ${props => props.theme.minimumSpacing};
    font-size: ${props => props.theme.minimumFontSize};
    left: 0;
`

const LeftButtonStyle = styled.div`
    background: none;
`

const RightButtonStyle = styled.div`
    background: ${props => props.theme.primaryColor};
`

const BottomBar = (props) => {
    
    return (
        <BottomBarContainer>
        <BottomMessageContainer>{props.bottomBarText}</BottomMessageContainer>
            <RightButtonStyle>
                <ButtonStyle>{props.rightButtonText}</ButtonStyle>
            </RightButtonStyle>
            <LeftButtonStyle>
                <ButtonStyle>{props.leftButtonText}</ButtonStyle>
            </LeftButtonStyle>
        </BottomBarContainer>
    )
}

export {BottomBar}

