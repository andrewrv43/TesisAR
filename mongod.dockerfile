FROM mongo:latest
COPY init-mongo.js /docker-entrypoint-initdb.d/
EXPOSE 80
CMD ["mongod", "--port", "80"]