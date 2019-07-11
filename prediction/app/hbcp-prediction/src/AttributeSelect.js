import React, { Component } from 'react';

class AttributeSelect extends Component {

    constructor(props) {
        super(props);
        // props should contain "endpoint", a list of strings mapping to valid API URLs
        // and a callback function called "onSelectChange"
        this.state = {
            options: [],
            value: { id: 0, name: "" },
        };
        this.handleChange = this.handleChange.bind(this);
    }

    handleChange(event) {
        const newValue = this.state.options.find(o => o.id === event.target.value);
        this.setState( (previous, props) => {
            previous.value = newValue;
            return previous;
        });
        this.props.onSelectChange(newValue);
    }

    componentDidMount() {
        let populationOptions = [];
        fetch(this.props.endpoint)
            .then(response => {
                return response.json();
            }).then(data => {
            populationOptions = data.map((option) => {
                return option
            });
            console.log(populationOptions);
            this.setState({
                options: populationOptions,
                value: populationOptions[0]
            });
            this.props.onSelectChange(populationOptions[0]);
        });
        let genderOptions = [];
        fetch(this.props.endpointGender)
            .then(response => {
                return response.json();
            }).then(data => {
            genderOptions = data.map((option) => {
                return option
            });
            console.log(genderOptions);
            this.setState({
                options: genderOptions,
                value: genderOptions[0]
            });
            this.props.onSelectChange(genderOptions[0]);
        });
        let interventionOptions = [];
        fetch(this.props.endpoint2)
            .then(response => {
                return response.json();
            }).then(data => {
            interventionOptions = data.map((option) => {
                return option
            });
            console.log(interventionOptions);
            this.setState({
                options: interventionOptions,
                value: interventionOptions[0]
            });
            this.props.onSelectChange(interventionOptions[0]);
        });
        
        let expOptions = [];
        
        fetch(this.props.endpoint3)
            .then(response => {
                return response.json();
            }).then(data => {
            expOptions = data.map((option) => {
                return option
            });
            console.log(expOptions);
            this.setState({
                options: expOptions,
                value: expOptions[0]
            });
            this.props.onSelectChange(expOptions[0]);
        });
        
        
        
    }

    render () {
        let options = this.state.options;
        let optionItems = options.map((option) =>
                <option value={option.id}>{option.name}</option>
            );

        return (
         <div class="select">
             <select onChange={this.handleChange} value={this.state.value.id} disabled={this.props.disabled}>
                {optionItems}
             </select>
         </div>
        )
    }
}

export default AttributeSelect;