import os
import logging
from logging.handlers import RotatingFileHandler

class Config:
    MONGO_URI = os.getenv('MONGO_URI')
    SECRET_KEY = os.getenv('pwdLocal')
    
    @staticmethod
    def init_app(app):
        pass
