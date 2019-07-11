import React, { Component } from 'react';
//import ReactDOM from 'react-dom';
//import logo from './logo.svg';
import './App.css';
import AttributeSelect from './AttributeSelect';
import List from './List';
import SearchResults from './SearchResults';
import Prediction from './Prediction';
import logo from './logo.png';
import Banner from 'react-banner'
import Headroom from 'react-headroom' // Import it
import 'react-banner/dist/style.css'



class App extends Component {
    constructor(props) {
        super(props);
        this.state = {
            populationAttributeInfo: null,
            interventionAttributeInfo: null,
            genderAttributeInfo: null,
            expAttributeInfo:null,

            queryList: [],
        };
        this.addItem = this.addItem.bind(this);
        this.addItemBCT = this.addItemBCT.bind(this);
        
        this.addItemGender = this.addItemGender.bind(this);
        this.addItemExp = this.addItemExp.bind(this);
        this.removeItem = this.removeItem.bind(this);
        this.selectedPopulationChange = this.selectedPopulationChange.bind(this);
        this.selectedBCTChange = this.selectedBCTChange.bind(this);
        this.selectedExpChange = this.selectedExpChange.bind(this);
        this.selectedGenderChange = this.selectedGenderChange.bind(this);

        this.predictOutcome = this.predictOutcome.bind(this);
    }

  addItem(e) {
    // Prevent button click from submitting form
    e.preventDefault();

    const newItem = document.getElementById("addInput");
    const newItemValue = newItem.value; // save the value before it's reset
    const form = document.getElementById("addItemForm");

    // If our input has a value
    if (newItemValue !== "") {
      // update the state with the new item
      this.setState((prev, props) => {
        let queryList = prev.queryList.slice();
        let newElement = {
            attributeInfo: prev.populationAttributeInfo,
            value: newItemValue,
            // + a text field
        };
        newElement.text = newElement.attributeInfo.name + " (" + newElement.attributeInfo.type + ") : " + newElement.value;
        queryList.push(newElement);
        prev.queryList = queryList; // not sure if this is necessary
        return prev;
      });
      // Finally, we need to reset the form
      newItem.classList.remove("is-danger");
      form.reset();
    } else {
      // If the input doesn't have a value, make the border red since it's required
      newItem.classList.add("is-danger");
    }
  }

addItemGender(e) {
    // Prevent button click from submitting form
    e.preventDefault();
    const form = document.getElementById("addItemForm");
    // update the state with the new item
    this.setState((prev, props) => {
        let queryList = prev.queryList.slice();
        let newElement = {
            attributeInfo: prev.genderAttributeInfo,
            value: 1.0,
            // + a text field
        };
        newElement.text = newElement.attributeInfo.name + " (" + newElement.attributeInfo.type + ") : " + newElement.value;
        queryList.push(newElement);
        prev.queryList = queryList; // not sure if this is necessary
        return prev;
    });
    form.reset();
  }
  addItemBCT(e) {
    // Prevent button click from submitting form
    e.preventDefault();
    const form = document.getElementById("addItemForm");
    // update the state with the new item
    this.setState((prev, props) => {
        let queryList = prev.queryList.slice();
        let newElement = {
            attributeInfo: prev.interventionAttributeInfo,
            value: 1.0,
            // + a text field
        };
        newElement.text = newElement.attributeInfo.name + " (" + newElement.attributeInfo.type + ") : " + newElement.value;
        queryList.push(newElement);
        prev.queryList = queryList; // not sure if this is necessary
        return prev;
    });
    form.reset();
  }

  addItemExp(e) {
    // Prevent button click from submitting form
    e.preventDefault();
    const form = document.getElementById("addItemForm");
    // update the state with the new item
    this.setState((prev, props) => {
        let queryList = prev.queryList.slice();
        let newElement = {
            attributeInfo: prev.expAttributeInfo,
            value: 1.0,
            // + a text field
        };
        newElement.text = newElement.attributeInfo.name + " (" + newElement.attributeInfo.type + ") : " + newElement.value;
        queryList.push(newElement);
        prev.queryList = queryList; // not sure if this is necessary
        return prev;
    });
    form.reset();
  }


  removeItem(item) {
    // Put our queryList into an array
    const queryList = this.state.queryList.slice();
    // Check to see if item passed in matches item in array
    queryList.some((el, i) => {
      if (el === item) {
        // If item matches, remove it from array
        queryList.splice(i, 1);
        return true;
      } else return false;
    });
    // Set state to queryList
    this.setState({
      queryList: queryList
    });
  }

  selectedPopulationChange(value) {
    // value should be the full object containing attribute ID and string representation
    // for now it's only a string
    this.setState((previous, props) => {
        previous.populationAttributeInfo = value;
        return previous;
    });
  }

    selectedBCTChange(value) {
    // value should be the full object containing attribute ID and string representation
    // for now it's only a string
    this.setState((previous, props) => {
        previous.interventionAttributeInfo = value;
        return previous;
    });
  }
  
 selectedGenderChange(value) {
    // value should be the full object containing attribute ID and string representation
    // for now it's only a string
    this.setState((previous, props) => {
        previous.genderAttributeInfo = value;
        return previous;
    });
  }

 selectedExpChange(value) {
    // value should be the full object containing attribute ID and string representation
    // for now it's only a string
    this.setState((previous, props) => {
        previous.expAttributeInfo = value;
        return previous;
    });
  }


  predictOutcome() {
    // this object will query the API and display the results
    //ReactDOM.render(<SearchResults query={this.state.queryList} />, document.getElementById('results'));
  }

  render() {
    return (
      <div class="content">
          <Headroom>
            <Banner
                logo=""
                url={ window.location.pathname }
                links={[
                    { "title": "HBCP", "url": "https://www.humanbehaviourchange.org/" },
                    { "title": "GitHub", "url": "https://github.com/HumanBehaviourChangeProject/Info-extract" },
                    { "title": "HBCP@IBMResearch", "url": "https://researcher.watson.ibm.com/researcher/view_group.php?id=8205"},
                    {/* "title": "Link w/ Children", "url": "/children", "children": [
                        { "title": "John", "url": "/children/john" },
                        { "title": "Jill", "url": "/children/jill" },
                        { "title": "Jack", "url": "/children/jack" }
                    ]*/}
                ]} />
        </Headroom>
        <div class="container"><nav class="level">
            <div class="level-left"><div class="level-item"><form className="form" id="addItemForm">
                <div id="text"> 
                    <h6>Smoking cessation behaviour change intervention inference: <br/></h6>
                Please select the features of your population and the BCT you would like to test on that population
                </div>
                <div id="queryItemAdders" class="columns">
                  <div class="column">
                    <div id="left" class="field has-addons">
                        <div  class="control">
                            <p>Population's Age</p>

                            <AttributeSelect endpoint='/com/ibm/drl/hbcp/api/predict/options/population' onSelectChange={this.selectedPopulationChange} />
                        </div>
                        <div class="control is-expanded">
                            <input
                                type="text"
                                className="input"
                                id="addInput"
                                placeholder="Attribute value..."
                              />
                        </div>
                        <div class="control">
                            <button className="button is-info" onClick={this.addItem}>
                                +
                            </button>
                        </div>
                    </div>
                  </div> 
                  <div class="column">
                    <div id="right" class="field has-addons">
                        <div  class="control">
                            <p>Gender</p>

                            <AttributeSelect endpointGender='/com/ibm/drl/hbcp/api/predict/options/gender' onSelectChange={this.selectedGenderChange} />

                        </div>
                        
                        <div class="control">
                            <button className="button is-info" onClick={this.addItemGender}>
                                +
                            </button>
                        </div>
                    </div>
                  </div>
                  <div class="column">
                    <div id="right" class="field has-addons">
                        <div class="control">
                            <p>BCTs</p>
                            <AttributeSelect endpoint2='/com/ibm/drl/hbcp/api/predict/options/intervention' onSelectChange={this.selectedBCTChange} />
                        </div>
                        <div class="control">
                            <button className="button is-info" onClick={this.addItemBCT}>
                                +
                            </button>
                        </div>
                    </div>
                  </div>
                  <div class="column">
                    <div id="right" class="field has-addons">
                        <div class="control">
                        <p>Experimental Settings</p>
                            <AttributeSelect endpoint3='/com/ibm/drl/hbcp/api/predict/options/expsettings' onSelectChange={this.selectedExpChange} disabled={true} />
                        </div>
                        <div class="control">
                            <button className="button is-info" onClick={this.addItemExp} disabled>
                                +
                            </button>
                        </div>
                    </div>
                  </div>
                </div>
            </form></div></div>
            <div class="level-right">
                <div class="level-item">
                    <figure id="logo">
                      <img src={logo} alt="HBCP" width="120" />
                    </figure>
                </div>
            </div>
            </nav>
            <List items={this.state.queryList} delete={this.removeItem} />
            <hr />
            <div id="res">
                <Prediction query={this.state.queryList} delay={2000}/>
            </div>
        </div>
     </div>
     );
  }
}

export default App;
