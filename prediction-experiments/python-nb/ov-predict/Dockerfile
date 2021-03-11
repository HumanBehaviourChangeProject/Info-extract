# Docker container with a bootstrapped installation of conda and python 3.6 that is ready to use
FROM conda/miniconda3
# force UTF-8
ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8
# Flask-on-Waitress API port
EXPOSE 5000
# Copy all the python/model files into the image
COPY . /prediction-experiments/python-nb/ov-predict
COPY embfile4api.merged.vec /core/prediction/graphs/nodevecs/embfile4api.merged.vec
WORKDIR /prediction-experiments/python-nb/ov-predict/src/api
# Create the Python env
RUN conda env create -f ../../hbcpPredictEnv.yml
# Make RUN commands use the new environment:
SHELL ["conda", "run", "-n", "hbcp-predict", "/bin/bash", "-c"]
# Make sure the environment is activated:
RUN echo "Make sure flask is installed:"
RUN python -c "import flask"
# Serve the Flair-Flask API with the webserver Waitress
ENTRYPOINT ["conda", "run", "-n", "hbcp-predict", "python", "predict_app_dev.py"]
