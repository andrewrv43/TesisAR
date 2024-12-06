from flask import Flask
from flasgger import Swagger
from app.routes import user_blueprint
from app.config import Config
import logging
from logging.handlers import RotatingFileHandler
from datetime import datetime
from pytz import timezone

local_tz = timezone("America/Guayaquil")
class CustomFormatter(logging.Formatter):
    def formatTime(self, record, datefmt=None):
        utc_dt = datetime.utcfromtimestamp(record.created)
        local_dt = utc_dt.astimezone(local_tz)
        return local_dt.strftime(datefmt or "%Y-%m-%d %H:%M:%S")
app = Flask(__name__)

# Inicializar Swagger
swagger = Swagger(app)

# Registrar los blueprints
app.register_blueprint(user_blueprint)

# Inicializar la aplicación
def create_app():
    app.config.from_object(Config)
    Config.init_app(app)

    # Configurar logging
    formatter = CustomFormatter(
        "%(asctime)s - %(levelname)s - %(message)s",  # Formato del log
        datefmt="%Y-%m-%d %H:%M:%S"  # Formato de fecha y hora
    )
    file_handler = RotatingFileHandler("/app/logs/app.log", maxBytes=10000, backupCount=3)
    file_handler.setFormatter(formatter)

    stream_handler = logging.StreamHandler()
    stream_handler.setFormatter(formatter)

    logging.basicConfig(
        level=logging.INFO,
        handlers=[file_handler, stream_handler]
    )


    app.logger.info("Logging forced at initialization.")
    return app
