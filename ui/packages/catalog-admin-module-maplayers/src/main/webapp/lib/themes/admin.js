import lightBaseTheme from 'material-ui/styles/baseThemes/lightBaseTheme'
import { fromJS } from 'immutable'

export default fromJS(lightBaseTheme).mergeDeep({
  textField: {
    errorColor: '#E74C3C'
  },
  raisedButton: {
    disabledColor: '#DDDDDD'
  },
  tableRow: {
    selectedColor: '#DDDDDD'
  },
  palette: {
    textColor: '#777777',
    primary1Color: '#18BC9C',
    accent1Color: '#2C3E50',
    accent2Color: '#FFFFFF',
    backdropColor: '#ECF0F1',
    errorColor: '#E74C3C',
    warningColor: '#DC8201',
    successColor: '#18BC9C',
    canvasColor: '#FFFFFF',
    disabledColor: '#DDDDDD'
  }
}).toJS()
