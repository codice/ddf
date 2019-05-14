/**
 * Copyright (c) Connexta, LLC
 *
 * <p>Unlimited Government Rights (FAR Subpart 27.4) Government right to use, disclose, reproduce,
 * prepare derivative works, distribute copies to the public, and perform and display publicly, in
 * any manner and for any purpose, and to have or permit others to do so.
 */
import React from 'react'
import ListCreate from '../../react-component/presentation/list-create/list-create'

const Marionette = require('marionette')

const ListCreateView = Marionette.LayoutView.extend({
  template() {
    return (
      <ListCreate
        model={this.model}
        withBookmarks={this.options.withBookmarks}
      />
    )
  },
})

export default ListCreateView
