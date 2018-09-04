const React = require('react');

const Enzyme = require('enzyme');
const Adapter = require('enzyme-adapter-react-16');

const { expect } = require('chai');

Enzyme.configure({ adapter: new Adapter() });

const { Menu, MenuItem } = require('./menu');

const { shallow } = Enzyme;

describe('<Menu />', () => {
    it('should not throw an error with no children', () => {
        const wrapper = shallow(<Menu />);
    });

    it('should render the correct number of <MenuItem />s', () => {
        const wrapper = shallow(
            <Menu value="two">
                <MenuItem value="one" />
                <MenuItem value="two" />
                <MenuItem value="three" />
            </Menu>
        );
        expect(wrapper.find('MenuItem').length).to.equal(3);
    });

    it('should have the correct <MenuItem /> selected', () => {
        const wrapper = shallow(
            <Menu value="two">
                <MenuItem value="one" />
                <MenuItem value="two" />
                <MenuItem value="three" />
            </Menu>
        );
        expect(wrapper.find({ selected: true }).prop('value')).to.equal('two');
    });

    it('should select the right <MenuItem /> on click', (done) => {
        const onChange = (value) => {
            expect(value).to.equal('one');
            done();
        };
        const wrapper = shallow(
            <Menu value="two" onChange={onChange}>
                <MenuItem value="one" />
                <MenuItem value="two" />
                <MenuItem value="three" />
            </Menu>
        );
        wrapper.find({ value: 'one' }).prop('onClick')();
    });

    const table = [
        {
            events: [],
            state: 'one'
        },
        {
            events: ['ArrowDown'],
            state: 'two'
        },
        {
            events: ['ArrowUp'],
            state: 'three'
        },
        {
            events: ['ArrowDown', 'ArrowDown'],
            state: 'three'
        },
        {
            events: ['ArrowDown', 'ArrowDown', 'ArrowDown'],
            state: 'one'
        }
    ];

    const mockEvent = (code) => ({ code, preventDefault: () => {} });

    table.forEach(({ events, state }) => {
        it(`should equal value='${state}' after ${JSON.stringify(events)}`, (done) => {
            const onChange = (value) => {
                expect(value).to.equal(state);
                done();
            };

            const wrapper = shallow(
                <Menu value="two" onChange={onChange}>
                    <MenuItem value="one" />
                    <MenuItem value="two" />
                    <MenuItem value="three" />
                </Menu>
            );

            const listener = wrapper.find('DocumentListener').prop('listener');

            events.forEach((event) => {
                listener(mockEvent(event));
            });

            listener(mockEvent('Enter'));
        });
    });

    it('should activate <MenuItem /> on hover', () => {
        const wrapper = shallow(
            <Menu value="two">
                <MenuItem value="one" />
                <MenuItem value="two" />
                <MenuItem value="three" />
            </Menu>
        );
        expect(wrapper.state('active')).to.equal('one');
        wrapper.find({ value: 'two' }).prop('onHover')();
        expect(wrapper.state('active')).to.equal('two');
    });
});
