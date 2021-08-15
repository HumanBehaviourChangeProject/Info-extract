# Docker container running TensorFlow Serving to serve prediction models
FROM tensorflow/serving:2.5.1
# Copy the saved models from the training image
COPY --from=hbcp-prediction-experiments /python-nb/ov-predict/src/out /models/
# Copy the model config file
COPY --from=hbcp-prediction-experiments /python-nb/ov-predict/saved_models/models.config /models/models.config
# TensorFlow Serving REST API port
EXPOSE 8501
# Serve the Flair-Flask API with the webserver Waitress
CMD ["tensorflow/serving", "--model_config_file=/models/models.config"]
