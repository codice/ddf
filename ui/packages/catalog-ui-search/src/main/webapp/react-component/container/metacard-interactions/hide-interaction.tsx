import * as React from 'react'
import { MetacardInteraction } from '../../presentation/metacard-interactions/metacard-interactions'
import { Props, Result, Model } from '.'
import * as user from '../../../component/singletons/user-instance'
import { hot } from 'react-hot-loader'

const handleShow = (context: Props) => {
  context.onClose()
  const preferences = user.get('user').get('preferences')
  const getResult = (result: Result) =>
    result
      .get('metacard')
      .get('properties')
      .get('id')

  preferences.get('resultBlacklist').remove(context.model.map(getResult))
  preferences.savePreferences()
}

const handleHide = (context: Props) => {
  context.onClose()
  const preferences = user.get('user').get('preferences')
  const getResult = (result: Result) => ({
    id: result
      .get('metacard')
      .get('properties')
      .get('id'),
    title: result
      .get('metacard')
      .get('properties')
      .get('title'),
  })

  preferences.get('resultBlacklist').add(context.model.map(getResult))
  preferences.savePreferences()
}
export const isBlacklisted = (model: Model): boolean => {
  const blacklist = user
    .get('user')
    .get('preferences')
    .get('resultBlacklist')
  return model.reduce((accum: boolean, result: Result) => {
    const id = result
      .get('metacard')
      .get('properties')
      .get('id')
    return blacklist.get(id) !== undefined || accum
  }, false)
}

const BlacklistToggle = (props: any) => {
  if (props.blacklisted) {
    return (
      <MetacardInteraction
        text="Unhide from Future Searches"
        help="Removes from the
                list of results that are hidden from future searches."
        icon="fa fa-eye"
        onClick={() => handleShow(props)}
      />
    )
  } else {
    return (
      <MetacardInteraction
        text="Hide from Future Searches"
        help="Adds to a list
                of results that will be hidden from future searches.  To clear this list,
                click the Settings icon, select Hidden, then choose to unhide the record."
        icon="fa fa-eye-slash"
        onClick={() => handleHide(props)}
      />
    )
  }
}

export default hot(module)(BlacklistToggle)
