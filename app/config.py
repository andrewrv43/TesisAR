import os
import logging
from logging.handlers import RotatingFileHandler

class Config:
    MONGO_URI = os.getenv('MONGO_URI', 'mongodb://localhost:27017/sp_base')
    SECRET_KEY = os.getenv('pwdLocal')
    
    @staticmethod
    def init_app(app):
        pass
