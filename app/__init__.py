from flask import Flask
from flasgger import Swagger
from app.routes import user_blueprint

app = Flask(__name__)

# Inicializar Swagger
swagger = Swagger(app)

# Registrar los blueprints
app.register_blueprint(user_blueprint)

# Inicializar la aplicación
def create_app():
    return app
