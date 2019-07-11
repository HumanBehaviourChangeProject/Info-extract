import React, { Component } from 'react';

class SearchResults extends Component {

    constructor(props) {
        super(props);
        // props should contain "endpoint", a list of strings mapping to valid API URLs
        // and a callback function called "onSelectChange"
        this.state = {
            results: []
        };
        this.formQuery = this.formQuery.bind(this);
        this.Result = this.Result.bind(this);
        this.areQueriesIdentical = this.areQueriesIdentical.bind(this);
    }

    componentDidUpdate(prevProps) {
        // Typical usage (don't forget to compare props):
        //if (!this.areQueriesIdentical(this.props.query, prevProps.query)) {
        if (!this.areQueriesIdentical(this.props.query, prevProps.query)) {
            let results = [];
            // fake results to signal the UI to display some spinning wheel
            this.setState({
                results: results,
                waiting: true
            });
            // delayed API call (this is truly beautiful) -->> Fra ::: :D:D:D
            setTimeout(() => {
                fetch(this.formQuery(this.props.query))
                     .then(response => {
                         return response.json();
                     })
                     .then(data => {
                         results = data.map((option) => {
                             return option
                         });
                         console.log(results);
                         this.setState({
                             results: results,
                             waiting: false
                         });
                     })
                     .catch((error) => {
                         console.log(error);
                     });
            }, this.props.delay);
        }
    }

    areQueriesIdentical(query1, query2) {
        if (query1.length === query2.length) {
            for (let i = 0; i < query1.length; i++) {
                if (query1[i].value !== query2[i].value)
                    return false;
                if (query1[i].attributeInfo.id !== query2[i].attributeInfo.id)
                    return false;
            }
            return true;
        } else return false;
    }

    formQuery(queryTerms) {
        const endpoint = "/com/ibm/drl/hbcp/api/predict/outcome?"
        var populationFound = "population=";
        var interventionFound = "intervention=";
        let args = queryTerms.map((term) => {
            let type = term.attributeInfo.type;
            let id = term.attributeInfo.id;
            let value = term.value;
            let argname = "";
            if (type === "C") {
                argname = "population=";
                populationFound = "";
            } else if (type === "I") {
                argname = "intervention=";
                interventionFound = "";
            } else { return ""; }
            return argname + type + ":" + id + ":" + value;
        });
        if (populationFound !== "")
            args.push(populationFound);
        if (interventionFound !== "")
            args.push(interventionFound);
        return endpoint + args.join("&");
        //return "/predict/outcome?population=&intervention=I:3675717:1.0";
    }

    Result(result) { return (
        <div class="box">
            <div class="columns">
                <div class="column is-2">
                    {result.rank}
                </div>
                <div class="column">
                    {result.item.attributeInfo.name} : {result.item.value}
                </div>
                <div class="column is-2">
                    {result.score}
                </div>
            </div>
        </div>
    );}

    render () {
        let results = this.state.results;
        let resultItems = results.map((r) => this.Result(r));

        return (
            <div class="lol">
                {resultItems}
            </div>
        )
    }
}

export default SearchResults;