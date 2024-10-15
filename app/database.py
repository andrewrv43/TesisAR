from pymongo import MongoClient

client = MongoClient('mongodb://localhost:80/')
db = client['sp_base']
