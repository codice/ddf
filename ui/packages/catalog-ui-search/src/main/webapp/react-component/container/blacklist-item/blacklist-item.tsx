/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
import { hot } from 'react-hot-loader'
import * as React from 'react'
import BlacklistItemPresentation from '../../presentation/blacklist-item'
const user = require('../../../component/singletons/user-instance.js')

var wreqr = require('../../../js/wreqr')

type Props = {
  item: Backbone.Model
}

const removeFromBlacklist = (id: string) => {
  user
    .get('user')
    .get('preferences')
    .get('resultBlacklist')
    .remove(id)
  user.savePreferences()
}

const navigateToItem = (id: string) => {
  wreqr.vent.trigger('router:navigate', {
    fragment: 'metacards/' + id,
    options: {
      trigger: true,
    },
  })
}

const BlacklistItemContainer = (props: Props) => {
  return (
    <BlacklistItemPresentation
      navigate={() => navigateToItem(props.item.id)}
      remove={() => removeFromBlacklist(props.item.id)}
      itemTitle={props.item.get('title')}
    />
  )
}

export default hot(module)(BlacklistItemContainer)
