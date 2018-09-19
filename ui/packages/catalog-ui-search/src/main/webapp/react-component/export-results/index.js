const React = require('react')

const {
  retrieveExportOptions,
  exportDataAs,
} = require('js/services/exportService')
const CustomElements = require('js/CustomElements')
const Component = CustomElements.registerReact('export-results')

const capitalize = value => value.charAt(0).toUpperCase() + value.slice(1)
const haveSearchResults = state =>
  state && state.activeSearchResults && state.activeSearchResults.length > 0

class ExportResults extends React.Component {
  constructor(props) {
    super(props)

    const passedProps = (props && props.options && props.options.props) || {}
    const activeSearchResults = {
      activeSearchResults:
        (props.state && props.state.activeSearchResults) || [],
    }
    const defaultExportSize = Object.keys(passedProps.export)[0]
    this.state = {
      ...passedProps,
      ...activeSearchResults,
      exportSizeValue: defaultExportSize,
      exportFormats: [],
    }
  }

  componentDidUpdate = async previousProps => {
    if (
      previousProps.state.activeSearchResults !==
      this.props.state.activeSearchResults
    ) {
      this.setState({
        activeSearchResults: this.props.state.activeSearchResults,
      })
    }
  }

  componentWillMount = async () => {
    if (
      this.state &&
      this.state.exportFormats &&
      this.state.exportFormats.length > 0
    ) {
      return
    }

    let jsonData

    try {
      const response = await retrieveExportOptions()
      jsonData = await response.json()
    } catch (error) {
      console.error(error)
      jsonData = []
    }

    this.setState({ exportFormats: jsonData })
  }

  logSuccess = response =>
    (this.state.onDownloadSuccess && this.state.onDownloadSuccess(response)) ||
    console.log(response)

  logError = error =>
    (this.state.onDownloadError && this.state.onDownloadError(error)) ||
    console.error(error)

  onDownloadClick = async () => {
    const forExport = this.state.export[this.state.exportSizeValue]
    const exportFormat =
      (this.state.exportFormatValue &&
        encodeURIComponent(this.state.exportFormatValue)) ||
      encodeURIComponent(this.state.defaultExportFormat)
    try {
      const url = `${forExport.url}${exportFormat}`
      const response = await exportDataAs(
        url,
        forExport.data(),
        this.state.contentType
      )

      this.logSuccess(response)
    } catch (error) {
      this.logError(error)
    }
  }

  handleExportSizeChange = event => {
    this.setState({ exportSizeValue: event.target.value })
  }

  handleExportFormatChange = event => {
    this.setState({ exportFormatValue: event.target.value })
  }

  renderExportSizeOption = exportOption => {
    return (
      <option key={exportOption} value={exportOption}>
        {capitalize(exportOption)}
      </option>
    )
  }

  renderExportFormatOption = key => {
    return (
      (key && (
        <option key={key} value={key}>
          {key.toUpperCase()}
        </option>
      )) ||
      null
    )
  }

  render = () => {
    return (
      (haveSearchResults(this.state) && (
        <Component>
          {` Export `}
          <select
            value={this.state.exportSizeValue}
            onChange={this.handleExportSizeChange}
            className="export-set-selection"
          >
            {Object.keys(this.state.export).map(this.renderExportSizeOption)}
          </select>
          {` as `}
          <select
            value={
              this.state.exportFormatValue || this.state.defaultExportFormat
            }
            onChange={this.handleExportFormatChange}
            className="export-format-selection"
          >
            {this.state.exportFormats.map(this.renderExportFormatOption)}
          </select>
          <button onClick={this.onDownloadClick} className="is-button">
            <span className="fa fa-download" />
          </button>
        </Component>
      )) ||
      null
    )
  }
}

module.exports = ExportResults
