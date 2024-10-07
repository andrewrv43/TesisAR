from flask import Flask
from flasgger import Swagger
from app.routes import user_blueprint
from app.config import Config
import logging
from logging.handlers import RotatingFileHandler

app = Flask(__name__)

# Inicializar Swagger
swagger = Swagger(app)

# Registrar los blueprints
app.register_blueprint(user_blueprint)

# Inicializar la aplicación
def create_app():


    logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(message)s', handlers=[
        RotatingFileHandler("app.log", maxBytes=10000, backupCount=3),
        logging.StreamHandler()
    ])

    app.logger.info("Logging forced at initialization.")
    return app
