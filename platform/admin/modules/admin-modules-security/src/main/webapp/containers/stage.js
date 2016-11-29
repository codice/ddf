import React from 'react'

import { connect } from 'react-redux'
import Stage from '../components/stage'
import Errors from '../containers/errors'

import Mount from '../components/mount'
import { fetch } from '../actions'

import { getAllStages } from '../reducer'

const AllStages = ({ stages = [], fetch, params }) => {
  return (
    <Mount key={params.stageId} on={() => fetch(params.stageId)}>
      <Errors />
      {stages.map((stage, i) => <Stage key={i} disabled={i > 0} {...stage} path={[ i ]} />).reverse()}
    </Mount>
  )
}

const mapStateToProps = (state) => ({ stages: getAllStages(state) })

export default connect(mapStateToProps, { fetch })(AllStages)
