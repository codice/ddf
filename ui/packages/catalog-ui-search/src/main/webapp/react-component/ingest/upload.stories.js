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
import  React, { useState } from 'react'
import { storiesOf, number, text } from '../storybook'

import {UploadView} from './upload-or-create.view'
import {EditItemView} from './edit-item.view'

const stories = storiesOf('IngestUpload', module)

stories.add('upload view', () => {
    const progress = number('Progress Amount', 50)
    return (
        <UploadView/>
    )
})

stories.add('edit item view', () => {
    const progress = number('Progress Amount', 50)
    return (
        <EditItemView/>
    )
})

