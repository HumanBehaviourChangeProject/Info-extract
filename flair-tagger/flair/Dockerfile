# Docker container with a bootstrapped installation of conda and python 3.6 that is ready to use
FROM conda/miniconda3
# Flask-on-Waitress API port
EXPOSE 8080
# Copy all the python/model files into the image
COPY . /app
WORKDIR /app
# Download model if not present
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*
RUN wget https://github.com/HumanBehaviourChangeProject/Info-extract/releases/download/0.9.0/best-model.pt -q --show-progress --progress=bar:force:noscroll -O best-model.pt
# Create the Flair Python env
RUN conda env create -f flairExpEnv.yml
# Make RUN commands use the new environment:
SHELL ["conda", "run", "-n", "flairExp", "/bin/bash", "-c"]
# Make sure the environment is activated:
RUN echo "Make sure flask is installed:"
RUN python -c "import flask"
# Serve the Flair-Flask API with the webserver Waitress
ENTRYPOINT ["conda", "run", "-n", "flairExp", "python", "app.py"]