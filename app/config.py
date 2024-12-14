import os
import logging
from logging.handlers import RotatingFileHandler
from .mail import EmailSender

class Config:
    MONGO_URI = os.getenv('MONGO_URI')
    SECRET_KEY = os.getenv('pwdLocal')
    email=EmailSender(os.getenv('pwdmail'))
    @staticmethod
    def init_app(app):
        Config.email.sendEmail("Service up","Se levantó el servicio con Exito")
        pass
