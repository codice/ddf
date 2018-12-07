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

const renderCategory = (
  withCloseDropdown: (action: () => void) => void,
  category: Category
) => (
  <React.Fragment key={`category-${category.name}`}>
    {category.items.map(item => renderLink(withCloseDropdown, item))}
    <div className="is-divider composed-menu" />
  </React.Fragment>
)

const renderLink = (
  withCloseDropdown: (action: () => void) => void,
  link: Link
) => (
  <div
    key={`key-${link.parent}-${link.linkText}`}
    className={`metacard-interaction ${link.parent}`}
    data-help={link.dataHelp}
    onClick={() => withCloseDropdown(link.actionHandler)}
  >
    <div className={`interaction-icon ${link.icon}`} />
    <div className="interaction-text">{link.linkText}</div>
  </div>
)

export const render = (props: Props) => (
  <>
    {props.viewModel.categories.map(category =>
      renderCategory(props.withCloseDropdown, category)
    )}
    <div
      className="metacard-interaction interaction-download"
      data-help="Downloads the result's associated product directly to your machine."
      onClick={props.handleDownload}
    >
      <div className="interaction-icon fa fa-download" />
      <div className="interaction-text">Download</div>
      {props.isRemoteResourceCached && (
        <span
          data-help="Displayed if the remote resource has been cached locally."
          className="download-cached"
        >
          Local
        </span>
      )}
    </div>
    <div
      className="metacard-interaction interaction-create-search"
      data-help="Uses the geometry of the metacard to populate a search."
      onClick={props.handleCreateSearch}
    >
      <div className="interaction-icon fa fa-globe" />
      <div className="interaction-text">Create Search from Location</div>
    </div>
  </>
)
