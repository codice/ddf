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
import * as React from 'react';

interface Props {
    help: string,
    icon?: string,
    iconText?: string,
    description: string,
    onClick: () => void;
}

const WorkspaceTemplate =  (props: Props) => {
    const { help, icon, iconText, description, onClick } = props;
    return (
        <div className="home-templates-choices-choice" onClick={onClick} data-help={help}>
            <div className={`${icon} home-templates-choices-choice-preview ${icon ? 'home-templates-choices-choice-preview-icon' : ''}`}>{iconText}</div>
            <div className="home-templates-choices-choice-description">{description}</div>
        </div>
    )
}

export default WorkspaceTemplate