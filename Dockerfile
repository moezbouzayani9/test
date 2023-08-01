FROM ${docker.image.base}

# HTTP port
EXPOSE 8080

COPY --chown=app config "$APP_HOME/config"
COPY --chown=app app/* "$APP_HOME"
