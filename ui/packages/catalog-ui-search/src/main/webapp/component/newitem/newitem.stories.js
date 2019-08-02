import { storiesOf } from '@connexta/ace/@storybook/react'
import React from 'react'
const BuilderStart = require('../builder/builder-start.view')

const Newitemview = './newitem.view'
const stories = storiesOf('New Item', module)

stories.add('Creation Page', () => {
    return (
        <BuilderStart/>
    )
})