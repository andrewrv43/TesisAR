import os
import logging
from logging.handlers import RotatingFileHandler

class Config:
    MONGO_URI = os.getenv('MONGO_URI')
    SECRET_KEY = os.getenv('pwdLocal')
    print(f"ESTA ES LA URI DEL MONGO {MONGO_URI}")
    @staticmethod
    def init_app(app):
        pass
