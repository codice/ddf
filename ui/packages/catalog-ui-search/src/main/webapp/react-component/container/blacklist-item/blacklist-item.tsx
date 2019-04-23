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
import BlacklistItemPresentation from '../../presentation/blacklist-item'
const user = require('../../../component/singletons/user-instance.js')

const wreqr = require('../../../js/wreqr')

type Props = {
  item: Backbone.Model
}

type State = {
  clearing: boolean
}

class BlacklistItemContainer extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {
      clearing: false,
    }
  }
  navigateToItem = () => {
    wreqr.vent.trigger('router:navigate', {
      fragment: 'metacards/' + this.props.item.id,
      options: {
        trigger: true,
      },
    })
  }
  removeFromBlacklist = () => {
    this.setState({ clearing: true })
    setTimeout(() => {
      user
        .get('user')
        .get('preferences')
        .get('resultBlacklist')
        .remove(this.props.item.id)
      user.savePreferences()
      this.setState({ clearing: false })
    }, 250)
  }
  render() {
    return (
      <BlacklistItemPresentation
        onNavigate={this.navigateToItem}
        onRemove={this.removeFromBlacklist}
        itemTitle={this.props.item.get('title')}
        clearing={this.state.clearing}
      />
    )
  }
}

export default hot(module)(BlacklistItemContainer)
