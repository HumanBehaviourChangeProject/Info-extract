# Docker container running TensorFlow Serving to serve prediction models
FROM tensorflow/serving:2.5.1
# Copy the starting models
COPY --from=hbcp-prediction-experiments /python-nb/ov-predict/saved_models/. /tmp/models/
# Copy the starting model config file
COPY --from=hbcp-prediction-experiments /python-nb/ov-predict/saved_models/models.config_original /tmp/models.config
# TensorFlow Serving REST API port
EXPOSE 8501
ENV TF_CPP_MIN_LOG_LEVEL 1
# Override the parent image entrypoint (otherwise it's running the server right away)
ENTRYPOINT []
# Here the folder /models/ is a mounted volume, copy the config/models first there and start the server
CMD cp /tmp/models.config /models/models.config && cp -r /tmp/models/* /models/ && \
	tensorflow_model_server --port=8500 --rest_api_port=8501 \
	--model_config_file=/models/models.config --model_config_file_poll_wait_seconds=60 --file_system_poll_wait_seconds=60
