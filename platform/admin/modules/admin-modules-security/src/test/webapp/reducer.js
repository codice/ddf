import { check, property, gen } from 'testcheck'
import { submittingStart, submittingEnd, networkError } from 'actions'

import reducer, { isSubmitting, getCurrentStage, getErrors } from 'reducer'

const isValid = (state, action, nextState) => {
  switch (action.type) {
    case 'SUBMITTING_START':
      return isSubmitting(nextState) === true
    case 'SUBMITTING_END':
      return isSubmitting(nextState) === false
    case 'EDIT_VALUE':
      return action.value === getCurrentStage(nextState).form.children[0].value
    case 'SET_STAGE':
      return getCurrentStage(state) !== getCurrentStage(nextState)
    case 'ERROR':
      return getErrors(nextState) !== null
  }
  return true
}

const c = check(
  property(
    [gen.array(
      gen.oneOf([
        gen.return(submittingStart()),
        gen.return(submittingEnd()),
        gen.return(networkError())
      ])
    )],
    (actions) => {
      var state = reducer()

      const badState = actions.find((action) => {
        try {
          const nextState = reducer(state, action)

          if (isValid(state, action, nextState)) {
            state = nextState
          } else {
            return true
          }
        } catch (e) {
          console.error(e)
          process.exit(1)
        }
      })

      return badState === undefined
    }
  ),
  { times: 200, seed: 42 }
)

if (!c.result) {
  console.log(JSON.stringify(c, null, 2))
  process.exit(1)
}

