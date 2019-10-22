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
import fetch from '../../react-component/utils/fetch/index'
import styled from 'styled-components'
const Common = require('../../js/Common.js')
const user = require('../singletons/user-instance.js')

const Root = styled.div`
  display: block;
  position: relative;
  height: 3 * ${props => props.theme.minimumButtonSize};
  cursor: pointer;
`

const Div = styled.div`
  width: calc(100% - 2.75rem);
`

const DetailDate = styled.div`
  padding: 0rem ${props => props.theme.minimumSpacing};
  height: ${props => props.theme.minimumButtonSize};
  line-height: ${props => props.theme.minimumButtonSize};
  display: flex;
  flex-flow: row wrap;
  justify-content: flex-end;
`

const Date = styled.span`
  left: ${props => props.theme.minimumSpacing};
  height: inherit;
  line-height: inherit;
  text-align: right;
`
const Content = styled.div`
  background: rgba(255, 255, 255, 0.11);
  cursor: pointer;
  z-index: 1;
  position: relative;
  left: 0px;
  top: 0px;
  text-align: center;
  padding: 0px 0.625rem;
  display: flex;
  flex-direction: column;
`

const TextDiv = styled.div`
  padding: ${props => props.theme.minimumSpacing} 0rem;
  flex: 2;
`

const Text = styled.div`
  vertical-align: top;
  overflow: hidden;
  text-overflow: ellipsis;
`

const ButtonDiv = styled.div`
  padding: ${props => props.theme.minimumSpacing} 0rem;
  flex: 1;
`

const ButtonText = styled.button`
  padding: 0rem ${props => props.theme.minimumSpacing};
`

const Remove = styled.div`
  width: 2.75rem;
  height: 100%;
  position: absolute;
  right: 0px;
  top: 0px;
  overflow: hidden;
`

const RemoveButton = styled.button`
  width: ${props => props.theme.minimumButtonSize};
  height: 100%;
  transform: translateX(0%);
  transition: transform ${props => props.theme.coreTransitionTime} linear;
`

type Props = {
  model: Backbone.Model
}

const OauthItem = ({ model }: Props) => {
  const modelJSON = model.toJSON()
  const when = Common.getMomentDate(modelJSON.when)

  let buttonText: String
  if (modelJSON.type === 'login') {
    buttonText = 'Sign in'
  } else {
    buttonText = 'Authorize'
  }

  let detail: String
  if (modelJSON.type === 'login') {
    detail =
      'Unable to query the ' +
      modelJSON.sourceId +
      ' source. Please login or remove it from the query.'
  } else {
    detail =
      'Attempting to query the ' +
      modelJSON.sourceId +
      ' source. Do you authorize to query this source? If not please remove the source from the query.'
  }

  return (
    <Root>
      <Div>
        <DetailDate>
          <Date>{when.toString()}</Date>
        </DetailDate>
        <Content>
          <TextDiv>
            <Text>{detail}</Text>
          </TextDiv>
          <ButtonDiv>
            <button
              className="is-primary"
              onClick={() => {
                setTimeout(() => {
                  model.collection.remove(model)
                  user
                    .get('user')
                    .get('preferences')
                    .savePreferences()
                }, 250)

                if (modelJSON.type === 'login') {
                  window.open(modelJSON.url, '_blank')
                } else {
                  fetch(modelJSON.url)
                }
              }}
            >
              <ButtonText>{buttonText}</ButtonText>
            </button>
          </ButtonDiv>
        </Content>
      </Div>
      <Remove>
        <RemoveButton
          className="actions-remove is-negative"
          onClick={() => {
            setTimeout(() => {
              model.collection.remove(model)
              user
                .get('user')
                .get('preferences')
                .savePreferences()
            }, 250)
          }}
          style={{ height: '100%' }}
        >
          <span className="fa fa-minus" />
        </RemoveButton>
      </Remove>
    </Root>
  )
}

export default OauthItem
