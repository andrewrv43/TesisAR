from bson.objectid import ObjectId
from app.database import db
from datetime import datetime

class UserModel:
#########################################
#           Usuarios                   #
#########################################
    @staticmethod
    def get_all_users():
        users = db['sp_users'].find()
        return [{'id': str(user['_id']), 'user': user['user'], 'pwd': user['pwd'], 'fecha_creacion': user['fecha_creacion']} for user in users]

    @staticmethod
    def get_user_by_id(user_id):
        user = db['sp_users'].find_one({'_id': ObjectId(user_id)})
        if user:
            return {
                'id': str(user['_id']),
                'user': user['user'],
                'pwd': user['pwd'],
                'fecha_creacion': user['fecha_creacion']
            }
        return None

    @staticmethod    
    def get_user_by_username(user):
        user = db['sp_users'].find_one({'user': user})
        if user:
            return {
                'id': str(user['_id']),
                'user': user['user'],
                'pwd': user['pwd'],
                'fecha_creacion': user['fecha_creacion']
            }
        return False

    @staticmethod
    def create_user(username, pwd):
        new_user = {
            'user': username,
            'pwd': pwd,
            'fecha_creacion': datetime.utcnow().isoformat()
        }
        result = db['sp_users'].insert_one(new_user)
        return UserModel.get_user_by_id(result.inserted_id)

    @staticmethod
    def update_user(data):
        updated_user = {
            'user': data.get('user'),
            'pwd': data.get('pwd')
        }
        result = db['sp_users'].update_one({'_id': ObjectId(data.get('_id'))}, {'$set': updated_user})
        if result.matched_count:
            return UserModel.get_user_by_id(data.get('_id'))
        return None

    @staticmethod
    def delete_user(user_id):
        result = db['sp_users'].delete_one({'_id': ObjectId(user_id)})
        return result.deleted_count > 0

#########################################
#           Velocidad                   #
#########################################

class SpeedRecord:
    @staticmethod
    def  get_all_speed_records():
        records = db['sp_record'].find()
        return [{'id': str(spdRecord['_id']),
                 'direccion':spdRecord['direccion'],
                 'latitud': spdRecord['latitud'],
                 'longitud': spdRecord['longitud'],
                 'velocidad': spdRecord['velocidad'],
                 'fecha': spdRecord['fecha'],
                 'street_max_speed':spdRecord['street_max_speed']} for spdRecord in records]
    
    @staticmethod
    def  get_speed_record_by_id(speed_record_id):
        speed_record = db['sp_record'].find_one({'_id': ObjectId(speed_record_id)})
        if speed_record:
            return {
                'id': str(speed_record['_id']),
            }
        return {
                'error': 'Error al procesar la solicitud de Retorno, id inexistente',
            }
    
    
    @staticmethod
    def get_records_by_user(userid: str):
        records = db['sp_record'].find({'userid': userid}).sort('fecha',-1)
        return [
            {
                'id': str(record['_id']),
                'direccion': (
                                record.get('direccion') and
                                record['direccion'].get('nameValuePairs') and
                                record['direccion']['nameValuePairs'].get('properties') and
                                record['direccion']['nameValuePairs']['properties'].get('nameValuePairs') and
                                record['direccion']['nameValuePairs']['properties']['nameValuePairs'].get('name', None)
                            ),
                'velocidad': record.get('velocidad'),
                'fecha': record.get('fecha'),
                'street_max_speed': record.get('street_max_speed')
            }
            for record in records
        ]
    @staticmethod
    def create_speed_record(latitud:str,longitud:str,direccion ,speed:str,street_max_speed:str,fecha:str,userid:str):
        """Funcion de creacion de valores en el registro de velocidad por usuario
        
        Keyword arguments:
            latitud -- string
            longitud -- string
            direccion
            speed -- string
            street_max_speed -- string
            fecha -- string

        Return: 
        dict -- Diccionario donde esta el id creado
        """
        
        newRecord={
            'latitud':latitud,
            'longitud':longitud,
            'direccion':direccion,
            'velocidad':speed,
            'street_max_speed':street_max_speed,
            'fecha':fecha,
            'userid':userid
        }
        record= db['sp_record'].insert_one(newRecord)
        return SpeedRecord.get_speed_record_by_id(record.inserted_id)
        
    def upload_many_data(new_records):
        """Funcion de carga de datos en el registro de velocidad por usuario"""
        if new_records:
            db['sp_record'].insert_many(new_records)
            saved_count = len(new_records)
            print(saved_count)
            return saved_count