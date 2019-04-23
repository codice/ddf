const Enzyme = require('enzyme')
const Adapter = require('enzyme-adapter-react-16')

const { expect } = require('chai')

Enzyme.configure({ adapter: new Adapter() })

const Dropdown = require('./dropdown')

const { shallow } = Enzyme

describe('<Dropdown />', () => {
  const Mock = () => null

  it('should render closed', () => {
    const wrapper = shallow(
      <Dropdown>
        <Mock />
      </Dropdown>
    )
    expect(wrapper.find(Mock).length).to.equal(0)
  })

  it('should render label', () => {
    const wrapper = shallow(
      <Dropdown label="test">
        <Mock />
      </Dropdown>
    )
    expect(wrapper.find({ children: 'test' })).to.have.length(1)
  })
})
