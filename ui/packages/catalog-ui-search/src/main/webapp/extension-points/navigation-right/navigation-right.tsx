import plugin from 'plugins/navigation-right'

import {
  Help,
  Settings,
  Notifications,
  User,
} from '../../react-component/presentation/navigation-right/buttons'

import { SFC } from '../../react-component/hoc/utils'

const DefaultItems = [Help, Settings, Notifications, User]

export default plugin(DefaultItems) as SFC<any>[]
