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
import * as React from 'react'

type Link = {
  parent: string
  icon: string
  dataHelp: string
  linkText: string
  actionHandler: () => void
}

type Category = {
  name: string
  items: Array<Link>
}

export type Model = {
  categories: Array<Category>
}

type Props = {
  viewModel: Model
  handleDownload: () => void
  handleCreateSearch: () => void
  withCloseDropdown: (action: () => void) => void
  isRemoteResourceCached: boolean
}

export const render = (props: Props) => (
  <>
    {props.viewModel.categories.map(category => {
      return (
        <React.Fragment key={`category-${category.name}`}>
          {category.items.map(link => {
            return (
              <div
                key={`key-${link.parent}-${link.linkText}`}
                className={`metacard-interaction ${link.parent}`}
                data-help={link.dataHelp}
                onClick={() => props.withCloseDropdown(link.actionHandler)}
              >
                <div className={`interaction-icon ${link.icon}`} />
                <div className="interaction-text">{link.linkText}</div>
              </div>
            )
          })}
          <div className="is-divider composed-menu" />
        </React.Fragment>
      )
    })}
  </>
)
