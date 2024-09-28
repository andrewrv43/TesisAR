from bson.objectid import ObjectId
from app.database import db
from datetime import datetime

class UserModel:
    @staticmethod
    def get_all_users():
        users = db['usrs'].find()
        return [{'id': str(user['_id']), 'user': user['user'], 'pwd': user['pwd'], 'fecha_creacion': user['fecha_creacion']} for user in users]

    @staticmethod
    def get_user_by_id(user_id):
        user = db['usrs'].find_one({'_id': ObjectId(user_id)})
        if user:
            return {
                'id': str(user['_id']),
                'user': user['user'],
                'pwd': user['pwd'],
                'fecha_creacion': user['fecha_creacion']
            }
        return None

    @staticmethod
    def create_user(username, pwd):
        new_user = {
            'user': username,
            'pwd': pwd,
            'fecha_creacion': datetime.utcnow().isoformat()
        }
        result = db['usrs'].insert_one(new_user)
        return UserModel.get_user_by_id(result.inserted_id)

    @staticmethod
    def update_user(user_id, data):
        updated_user = {
            'user': data.get('user'),
            'pwd': data.get('pwd')
        }
        result = db['usrs'].update_one({'_id': ObjectId(user_id)}, {'$set': updated_user})
        if result.matched_count:
            return UserModel.get_user_by_id(user_id)
        return None

    @staticmethod
    def delete_user(user_id):
        result = db['usrs'].delete_one({'_id': ObjectId(user_id)})
        return result.deleted_count > 0
