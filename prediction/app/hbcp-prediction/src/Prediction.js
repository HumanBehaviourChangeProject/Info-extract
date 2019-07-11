import React, { Component } from 'react';
import SearchResults from './SearchResults';
import throbber from './throbber.gif';

class Prediction extends SearchResults {

    constructor(props) {
        super(props);

        this.populationString = this.populationString.bind(this);
        this.interventionString = this.interventionString.bind(this);
        this.bestOutcomeValue = this.bestOutcomeValue.bind(this);
    }

    populationString(queryTerms) {
        let cTerms = queryTerms.filter(term => {
            let type = term.attributeInfo.type;
            return type === "C";
        });
        let cStrings = cTerms.map(term => {
            return term.attributeInfo.name + " = " + term.value;
        });
        return cStrings.join(", ");
    }

    interventionString(queryTerms) {
        let iTerms = queryTerms.filter(term => {
            let type = term.attributeInfo.type;
            return type === "I";
        });
        let iStrings = iTerms.map(term => {
            return term.attributeInfo.name;
        });
        return iStrings.join(" or ");
    }

    bestOutcomeValue(results) {
        let result = results[0].item.value;
        // extract the first number in that string
        let number = parseFloat(result.replace( /(^.+)(\w\d+\w)(.+$)/i,'$2'));
        return Math.round(number * 100) / 100;
    }

    render () {
        // parts of the query to render
        let population = this.populationString(this.props.query);
        let populationPart = population === "" ? <someText>Applying </someText> : <someText> For a population of <font color="blue">{population}</font>, applying </someText>;
        let intervention = this.interventionString(this.props.query);
        let interventionPart = intervention === "" ? <someText> an appropriate behavior change technique </someText> : <font color="blue"> {intervention} </font>;
        // rendering of results
        let results = this.state.results;
        if (this.props.query.length === 0 && results.length === 0) {
            return <div />;
        } else if (intervention === "") {
            return <div class="result">Please add a BCT.</div>;
        } else if (this.state.waiting) {
            return (
                <nav class="level">
                  <div class="level-item has-text-centered">
                    <figure class='image is-128x128'><img src={throbber} width="120" /></figure>
                  </div>
                </nav>
            ); //<a class="button is-loading">Loading</a>;
        } else if (results.length === 0) {
            return <div class="result">No outcome could be predicted for this setting.</div>;
        } else {
            let outcomeValue = this.bestOutcomeValue(results);
            return (
                <div class="result">
                    <div class="box">
                        {populationPart} {interventionPart} is likely to lead
                        <font color="green"><b> {outcomeValue}% </b></font>
                        of patients to stop smoking.
                    </div>
                </div>
            )
        }
    }
}

export default Prediction;