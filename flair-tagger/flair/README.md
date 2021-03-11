# FLAIR BCIO Entity Tagging API

## Requirements

* Docker, with sufficient resources (RAM>=2GB)
* A FLAIR model file (for developers, you can find one here: https://ibm.ent.box.com/folder/101867939283)

## Instructions

1. Copy the FLAIR model file to the `flair` directory
1. Open a terminal in the `flair` directory
1. `docker build .`
1. Copy the newly created image ID
1. `docker run -p 5000:8080 [image_ID]`

The FLAIR Entity Tagging API is now running on your machine available at port 5000.

## Testing the API

In a terminal, run:

`curl -d '{"sentence":"The enthusiasm, however, is much lower when it comes to promoting quitlines among Asian language speakers in the United States (13-15)."}' -H "Content-Type: application/json" -X POST http://localhost:5000/api/v1/extractEntitiesSingleSent`

This will normally return an HTTP response indicating that "United States" has been detected as a "Country of Intervention".
