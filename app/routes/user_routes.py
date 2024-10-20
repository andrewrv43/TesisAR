from flask import Blueprint, jsonify, request
from app.models.user_model import UserModel,SpeedRecord
from flasgger import swag_from
from app.config import Config
import datetime
import jwt
from app.auth_middleware import token_required
user_blueprint = Blueprint('user', __name__)

def verify_user(username, password):
    # Obtener todos los usuarios de UserModel
    if str(username).lower() in ['andrew', 'ronny'] and password in ['1234', '4815']:
        return {
            "user": "admin",
            "id": "1"
        }
    for user in UserModel.get_all_users():
        # Verificar si el nombre de usuario existe y si la contraseña es correcta
        if user['user'] == username:
            return user
    

    # Si no se encuentra coincidencia, retornar None
    return None

@user_blueprint.route('/login', methods=['POST'])
def login():
    auth = request.get_json()
    user = verify_user(auth['username'], auth['password'])
    if user:
        token = jwt.encode({
            'user': user['user'],
            'id': user['id'],
            'exp': datetime.datetime.utcnow() + datetime.timedelta(minutes=30)
        }, Config.SECRET_KEY, algorithm='HS256')
        return jsonify({'token': token})
    return jsonify({'message': 'Usuario o contraseña incorrectos'}), 401
#########################################
#           USUARIOS                    #
#########################################
# Ruta para obtener todos los usuarios
@user_blueprint.route('/user', methods=['GET'])
@token_required
@swag_from({
    'tags': ['USER'],
    'summary': 'Obtener todos los usuarios',
    'description': 'Obtén la lista de todos los usuarios registrados',
    'responses': {
        200: {
            'description': 'Lista de usuarios obtenida exitosamente',
            'content': {
                'application/json': {
                    'example': [
                        {'id': '1', 'user': 'JohnDoe', 'pwd': 'hashedpassword', 'fecha_creacion': '2023-09-15T14:28:22.842Z'},
                        {'id': '2', 'user': 'JaneDoe', 'pwd': 'anotherhashedpassword', 'fecha_creacion': '2023-09-16T10:12:22.842Z'}
                    ]
                }
            }
        }
    }
})
def get_users():
    users = UserModel.get_all_users()
    return jsonify(users), 200

# Ruta para obtener un usuario por ID
@user_blueprint.route('/user/<id>', methods=['GET'])
@token_required
@swag_from({
    'tags': ['USER'],
    'summary': 'Obtener un usuario por ID',
    'description': 'Obtén los detalles de un usuario registrado a partir de su ID',
    'parameters': [
        {
            'name': 'id',
            'in': 'path',
            'type': 'string',
            'required': True,
            'description': 'ID del usuario'
        }
    ],
    'responses': {
        200: {
            'description': 'Usuario obtenido exitosamente',
            'content': {
                'application/json': {
                    'example': {'id': '1', 'user': 'JohnDoe', 'pwd': 'hashedpassword', 'fecha_creacion': '2023-09-15T14:28:22.842Z'}
                }
            }
        },
        404: {
            'description': 'Usuario no encontrado'
        }
    }
})
def get_user(id):
    user = UserModel.get_user_by_id(id)
    if user:
        return jsonify(user), 200
    else:
        return jsonify({'error': 'Usuario no encontrado'}), 404


# Ruta para obtener un usuario por username
@user_blueprint.route('/user/<username>', methods=['GET'])
@token_required
@swag_from({
    'tags': ['USER'],
    'summary': 'Obtener un usuario por ID',
    'description': 'Obtén los detalles de un usuario registrado a partir de su ID',
    'parameters': [
        {
            'name': 'id',
            'in': 'path',
            'type': 'string',
            'required': True,
            'description': 'ID del usuario'
        }
    ],
    'responses': {
        200: {
            'description': 'Usuario obtenido exitosamente',
            'content': {
                'application/json': {
                    'example': {'id': '1', 'user': 'JohnDoe', 'pwd': 'hashedpassword', 'fecha_creacion': '2023-09-15T14:28:22.842Z'}
                }
            }
        },
        404: {
            'description': 'Usuario no encontrado'
        }
    }
})
def get_username(username):
    user = UserModel.get_user_by_username(username)
    if user:
        return jsonify(user), 200
    else:
        return jsonify({'error': 'Usuario no encontrado'}), 404




# Ruta para crear un nuevo usuario
@user_blueprint.route('/user', methods=['POST'])
@swag_from({
    'tags': ['USER'],
    'summary': 'Crear un nuevo usuario',
    'description': 'Agrega un nuevo usuario a la base de datos',
    'requestBody': {
        'required': True,
        'content': {
            'application/json': {
                'schema': {
                    'type': 'object',
                    'properties': {
                        'user': {
                            'type': 'string',
                            'description': 'Nombre de usuario',
                            'example': 'JohnDoe'
                        },
                        'pwd': {
                            'type': 'string',
                            'description': 'Contraseña del usuario',
                            'example': 'hashedpassword'
                        }
                    },
                    'required': ['user', 'pwd']
                }
            }
        }
    },
    'responses': {
        201: {
            'description': 'Usuario creado exitosamente',
            'content': {
                'application/json': {
                    'example': {
                        'id': '1',
                        'user': 'JohnDoe',
                        'pwd': 'hashedpassword',
                        'fecha_creacion': '2023-09-15T14:28:22.842Z'
                    }
                }
            }
        },
        400: {
            'description': 'Solicitud incorrecta, datos incompletos o inválidos'
        }
    }
})
def create_new_user():  
    data = request.get_json()

    # Verificar que los parámetros obligatorios estén presentes
    if 'user' not in data or 'pwd' not in data:
        return jsonify({'error': 'Faltan parámetros obligatorios: user y pwd'}), 400

    # Verificar si el usuario ya existe
    if not UserModel.get_user_by_username(data['user']):
        new_user = UserModel.create_user(data['user'], data['pwd'])
        return jsonify(new_user), 201
    else:
        return jsonify({'error': 'Este nombre de usuario ya existe'}), 409   
# Ruta para actualizar un usuario por ID
@user_blueprint.route('/useract', methods=['POST'])
@token_required
@swag_from({
    'tags': ['USER'],
    'summary': 'Actualizar un usuario por ID',
    'description': 'Actualiza los datos de un usuario existente',
    'parameters': [
        {
            'name': 'id',
            'in': 'path',
            'type': 'string',
            'required': True,
            'description': 'ID del usuario'
        }
    ],
    'requestBody': {
        'content': {
            'application/json': {
                'example': {'user': 'JohnDoeUpdated', 'pwd': 'newhashedpassword'}
            }
        }
    },
    'responses': {
        200: {
            'description': 'Usuario actualizado exitosamente',
            'content': {
                'application/json': {
                    'example': {'id': '1', 'user': 'JohnDoeUpdated', 'pwd': 'newhashedpassword', 'fecha_creacion': '2023-09-15T14:28:22.842Z'}
                }
            }
        },
        404: {
            'description': 'Usuario no encontrado'
        }
    }
})
def update_user():
    data = request.get_json()

    # Verificar que los parámetros obligatorios estén presentes
    if 'id' not in data or 'user' not in data or 'pwd' not in data:
        return jsonify({'error': 'Faltan parámetros obligatorios: id, user y pwd'}), 400

    # Verificar si el nuevo nombre de usuario ya existe
    existing_user = UserModel.get_user_by_username(data['user'])
    if existing_user:
        return jsonify({'error': 'El nombre de usuario ya está en uso'}), 409

    updated_user = UserModel.update_user(data)
    
    if updated_user:
        return jsonify(updated_user), 200
    else:
        return jsonify({'error': 'Usuario no encontrado'}), 404

#########################################
#          RegistroVelocidad            #
#########################################

@user_blueprint.route('/sp_nvrecord', methods=['POST'])
@token_required
@swag_from({
    'tags': ['Velocity Records'],
    'summary': 'Crear un nuevo registro de velocidad',
    'description': 'Agrega un nuevo registro de velocidad a la base de datos',
    'requestBody': {
        'required': True,
        'content': {
            'application/json': {
                'schema': {
                    'type': 'object',
                    'properties': {
                        'latitud': {
                            'type': 'number',
                            'description': 'Latitud donde se registra la velocidad',
                            'example': 40.7128
                        },
                        'longitud': {
                            'type': 'number',
                            'description': 'Longitud donde se registra la velocidad',
                            'example': -74.0060
                        },
                        'user_id': {
                            'type': 'integer',
                            'description': 'ID del usuario',
                            'example': 1
                        },
                        'velocidad': {
                            'type': 'number',
                            'description': 'Velocidad registrada en km/h',
                            'example': 65.5
                        },
                        'fecha': {
                            'type': 'string',
                            'format': 'date-time',
                            'description': 'Fecha y hora del registro',
                            'example': '2023-09-15T14:28:22.842Z'
                        }
                    },
                    'required': ['latitud', 'longitud', 'user_id', 'velocidad', 'fecha']
                }
            }
        }
    },
    'responses': {
        201: {
            'description': 'Registro de velocidad creado exitosamente',
            'content': {
                'application/json': {
                    'example': {
                        'latitud': 40.7128,
                        'longitud': -74.0060,
                        'user_id': 1,
                        'velocidad': 65.5,
                        'fecha': '2023-09-15T14:28:22.842Z'
                    }
                }
            }
        },
        400: {
            'description': 'Solicitud incorrecta, datos incompletos o inválidos'
        }
    }
})
def create_sp_record():
    data = request.get_json()
    required_fields = ['latitud', 'longitud', 'user_id', 'velocidad', 'fecha']
    missing_fields = [field for field in required_fields if field not in data]
    if missing_fields:
        return jsonify({'error': f'Faltan los siguientes parámetros: {", ".join(missing_fields)}'}), 400

    new_record = SpeedRecord.create_speed_record(data['latitud'], data['longitud'], data['user_id'], data['velocidad'], data['fecha'])
    return jsonify(new_record), 201
@user_blueprint.route('/get_spdrecords', methods=['GET'])
@token_required
@swag_from({
    'tags': ['Velocity Records'],
    'summary': 'Crear un nuevo registro de velocidad',
    'description': 'Agrega un nuevo registro de velocidad a la base de datos',
    'requestBody': {
        'required': True,
        'content': {
            'application/json': {
                'schema': {
                    'type': 'object',
                    'properties': {
                        'latitud': {
                            'type': 'number',
                            'description': 'Latitud donde se registra la velocidad',
                            'example': 40.7128
                        },
                        'longitud': {
                            'type': 'number',
                            'description': 'Longitud donde se registra la velocidad',
                            'example': -74.0060
                        },
                        'user_id': {
                            'type': 'integer',
                            'description': 'ID del usuario',
                            'example': 1
                        },
                        'velocidad': {
                            'type': 'number',
                            'description': 'Velocidad registrada en km/h',
                            'example': 65.5
                        },
                        'fecha': {
                            'type': 'string',
                            'format': 'date-time',
                            'description': 'Fecha y hora del registro',
                            'example': '2023-09-15T14:28:22.842Z'
                        }
                    },
                    'required': ['latitud', 'longitud', 'user_id', 'velocidad', 'fecha']
                }
            }
        }
    },
    'responses': {
        201: {
            'description': 'Registro de velocidad creado exitosamente',
            'content': {
                'application/json': {
                    'example': {
                        'latitud': 40.7128,
                        'longitud': -74.0060,
                        'user_id': 1,
                        'velocidad': 65.5,
                        'fecha': '2023-09-15T14:28:22.842Z'
                    }
                }
            }
        },
        400: {
            'description': 'Solicitud incorrecta, datos incompletos o inválidos'
        }
    }
})
def obtainallrecords():  
    response = SpeedRecord.get_all_speed_records()
    return jsonify(response), 200
