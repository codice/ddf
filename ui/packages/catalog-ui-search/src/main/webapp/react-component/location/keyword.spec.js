const React = require('react');

const Enzyme = require('enzyme');
const Adapter = require('enzyme-adapter-react-16');

const { expect } = require('chai');

Enzyme.configure({ adapter: new Adapter() });

const Keyword = require('./keyword');
const AutoComplete = require('../auto-complete');

const { shallow } = Enzyme;

describe('<Keyword />', () => {
    it('should fetch features on select', (done) => {
        const coordinates = [1, 2, 3];
        const fetch = async (url) => {
            expect(url).to.equal('./internal/geofeature?id=0');
            return {
                async json() {
                    return {
                        geometry: {
                            type: 'Polygon',
                            coordinates: [coordinates]
                        }
                    };
                }
            };
        };
        const setState = ({ polygon }) => {
            expect(wrapper.state('loading')).to.equal(false);
            expect(polygon).to.equal(coordinates);
            done();
        };
        const wrapper = shallow(<Keyword fetch={fetch} setState={setState} />);
        wrapper.find('AutoComplete').prop('onChange')({
            id: '0',
            name: 'test'
        });
        expect(wrapper.state('loading')).to.equal(true);
    });
    it('should inform user on failure to fetch geofeature', (done) => {
        const onError = () => {
            wrapper.update();
            expect(wrapper.text()).include('Geo feature endpoint unavailable');
            done();
        };
        const fetch = async () => {
            throw new Error('unavailable');;
        };
        const wrapper = shallow(<Keyword fetch={fetch} onError={onError} />);
        wrapper.find('AutoComplete').prop('onChange')({
            id: '0',
            name: 'test'
        });
    });
});
