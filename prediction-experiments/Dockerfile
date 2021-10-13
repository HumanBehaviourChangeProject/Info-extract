# Docker container with a bootstrapped installation of conda and python 3.6
FROM conda/miniconda3
# force UTF-8
ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8
# Flask-on-Waitress API port
EXPOSE 5000
# Mirror the folder structure in the image (not sure if needed)
COPY . .
# Copy the embedding and training files from the maven image
COPY --from=hbcp-core /prediction/resources /python-nb/ov-predict/resources
# Create the Python env
RUN conda update -y -n base -c defaults conda
RUN conda env create -f hbcpPredictEnv.yml
# Make RUN commands use the new environment:
SHELL ["conda", "run", "-n", "hbcp-predict", "/bin/bash", "-c"]
# Make sure the environment is activated:
RUN echo "Make sure flask is installed:"
RUN python -c "import flask"
# Train the prediction model
WORKDIR /python-nb/ov-predict/src/
RUN conda run -n hbcp-predict python train4api.py -i ../resources/train4api.tsv -n ../resources/embfile4api.merged.vec
# At this point models have been placed in /python-nb/ov-predict/saved_models/
# install the models TinyDB containing the initial models
RUN cp ../saved_models/models_tinydb.json_original ../saved_models/models_tinydb.json
# Serve the Flair-Flask API with the webserver Waitress
WORKDIR /python-nb/ov-predict/src/api
CMD ["conda", "run", "-n", "hbcp-predict", "--no-capture-output", "python", "predict_app_outcome_and_confidence.py"]
