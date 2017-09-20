import darkBaseTheme from 'material-ui/styles/baseThemes/darkBaseTheme'
import { fromJS } from 'immutable'

export default fromJS(darkBaseTheme).mergeDeep({
  raisedButton: {
    disabledColor: '#444444'
  },
  tableRow: {
    selectedColor: '#3A3B3E'
  },
  palette: {
    errorColor: '#8a423c',
    primary3Color: '#757575',
    disabledColor: '#444444',
    borderColor: '#3A3B3E',
    alternateTextColor: '#000000',
    backdropColor: '#35353a',
    canvasColor: '#28282c',
    secondaryTextColor: 'rgba(255, 255, 255, 0.7)',
    textColor: '#C0C0C0',
    accent3Color: '#ff80ab',
    primary2Color: '#0097a7',
    pickerHeaderColor: 'rgba(255, 255, 255, 0.12)',
    primary1Color: '#3c6dd5',
    successColor: '#3b773b',
    warningColor: '#c89600',
    accent1Color: '#B03D67',
    clockCircleColor: 'rgba(255, 255, 255, 0.12)',
    accent2Color: '#2A3943'
  }
}).toJS()
