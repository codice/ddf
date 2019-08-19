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
import React, { useState } from 'react'
import { storiesOf, number, text } from '../storybook'
import { InformalProductsTable } from './informal-upload-table'
import { ProgressBar, ProgressBarWithText } from './progress-car'

const stories = storiesOf('Informal Products', module)

stories.add('progress bar', () => {
  const progress = number('Progress Amount', 50)
  return <ProgressBar progress={progress} />
})

stories.add('progress bar with text', () => {
  const progress = number('progress amount', 74)
  const message = text('message', 'Stop')
  return <ProgressBarWithText progress={progress} message={message} />
})

stories.add('uploads table', () => {
  const uploads = [
    {
      title: 'Document 1',
      fileType: 'JIF LOREM EPSO DO DA UNUM',
      progress: 40,
      message: 'Stop',
    },
    {
      title: 'Document 2: Document Reloaded',
      fileType: 'NEO',
      progress: null,
      message: 'Failed. Try again?',
    },
    {
      title: 'Document 3: Document Revolutions',
      fileType: 'PDF',
      progress: 10,
      message: 'Duplicate Detected',
    },
  ]

  return <InformalProductsTable uploads={uploads} />
})
