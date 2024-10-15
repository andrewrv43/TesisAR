FROM python:3.9
WORKDIR /app
COPY requirements.txt requirements.txt
RUN pip install -r requirements.txt
COPY . .
COPY init-mongo.js /docker-entrypoint-initdb.d/
EXPOSE 80
CMD ["gunicorn", "-b", "0.0.0.0:5000", "run:app"]
