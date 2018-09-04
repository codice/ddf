const React = require('react');

const Group = require('react-component/group');
const CustomElements = require('js/CustomElements');
const Component = CustomElements.registerReact('list-editor');

class ListEditor extends React.Component{
    handleAdd() {
        const { list, defaultItem, onChange } = this.props;
        const newList = list.slice();
        newList.push(defaultItem);
        onChange(newList);
    }

    handleRemove(index) {
        const { list, onChange } = this.props;
        const newList = list.slice();
        newList.splice(index, 1);
        onChange(newList);
    }

    render() {
        const listItems = React.Children.map(this.props.children, (child, index) =>
            <li className="item">
                <Group>
                    {child}
                    <button className="button-remove is-negative" onClick={this.handleRemove.bind(this, index)} >
                        <span className="fa fa-minus" />
                    </button>
                </Group>
            </li>
        );
        return (
            <Component>
                <ul className="list">
                    {listItems}
                </ul>
                <button className="button-add is-positive" onClick={this.handleAdd.bind(this)} >
                    <span className="fa fa-plus" />
                </button>
            </Component>
        );
    }
}

module.exports = ListEditor;