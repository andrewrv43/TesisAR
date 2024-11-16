from email.message import EmailMessage
import ssl
import smtplib
import os


class EmailSender():
    def __init__(self, pwd):
        self.password=pwd
    def sendEmail(self,subject,body):
        mine = "andrewrv43@gmail.com"
        objetivo = ["andrewrv43@gmail.com",'ronnyrosero@gmail.com']
        for obj in objetivo:
            em = EmailMessage()
            em["From"] = mine
            em["To"] = obj
            em['Subject']=subject
            em.set_content(body)
            context=ssl.create_default_context()
            with smtplib.SMTP_SSL("smtp.gmail.com",465,context=context) as smtp:
                smtp.login(mine,self.password)
                smtp.sendmail(mine,obj,em.as_string())
        