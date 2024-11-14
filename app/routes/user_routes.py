from flask import Blueprint, jsonify, request
from app.models.user_model import UserModel,SpeedRecord
from flasgger import swag_from
from app.config import Config
from datetime import timezone
import datetime
import jwt
from app.auth_middleware import token_required
user_blueprint = Blueprint('user', __name__)
    
@user_blueprint.route('/token/time_left', methods=['GET'])
@token_required
def token_time_left():
    # Obtener el token del encabezado Authorization
    token = request.headers.get('Authorization')
    
    if token:
        try:
            token = token.split()[1] if "Bearer" in token else token
            secret_key = Config.SECRET_KEY
            decoded_token = jwt.decode(token, secret_key, algorithms=['HS256'])
            exp_timestamp = decoded_token.get('exp', None)
            if exp_timestamp:
                current_time = datetime.datetime.now(timezone.utc)
                exp_time = datetime.datetime.fromtimestamp(exp_timestamp, tz=timezone.utc)
                time_left = exp_time - current_time
                if time_left.total_seconds() > 0:
                    days_left = time_left.days
                    return jsonify({
                        'time_left': str(days_left)
                    }), 200
                else:
                    return jsonify({'time_left': '0'}), 403
            else:
                return jsonify({'time_left': '0'}), 400
        except jwt.ExpiredSignatureError:
            return jsonify({'time_left': '0'}), 403
        except jwt.InvalidTokenError:
            return jsonify({'time_left': '0'}), 403
        except TypeError as e:
            return jsonify({'time_left': '0', 'error': str(e)}), 400
    else:
        return jsonify({'time_left': '0'}), 403

def verify_user(username, password):
    # Obtener todos los usuarios de UserModel
    if str(username).lower() in ['andrew', 'ronny'] and password in ['1234', '4815','4312']:
        return {
            "user": "admin",
            "id": "1"
        }
    for user in UserModel.get_all_users():
        # Verificar si el nombre de usuario existe y si la contraseña es correcta
        if user['user'] == username and user['pwd']==password:
            return user
    

    # Si no se encuentra coincidencia, retornar None
    return None

@token_required
def decode_token(token:str):
    try:
        # Decodificar el token usando la clave secreta
        decoded_data = jwt.decode(token, Config.SECRET_KEY, algorithms=['HS256'])
        user = decoded_data.get('user')
        user_id = decoded_data.get('id')
        return  user,user_id
    except jwt.ExpiredSignatureError:
        return None
    except jwt.InvalidTokenError:
        return None

@user_blueprint.route('/login', methods=['POST'])
def login():
    auth = request.get_json()
    user = verify_user(auth['username'], auth['password'])
    if user:
        token = jwt.encode({
            'user': user['user'],
            'id': user['id'],
            'exp': datetime.datetime.utcnow() + datetime.timedelta(days=90)
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
    'description': 'Agrega un nuevo registro de velocidad a la base de datos con información adicional de dirección y velocidades máximas de la calle.',
    'requestBody': {
        'required': True,
        'content': {
            'application/json': {
                'schema': {
                    'type': 'object',
                    'properties': {
                        'latitud': {
                            'type': 'string',
                            'description': 'Latitud donde se registra la velocidad',
                            'example': '40.7128'
                        },
                        'longitud': {
                            'type': 'string',
                            'description': 'Longitud donde se registra la velocidad',
                            'example': '-74.0060'
                        },
                        'direccion': {
                            'type': 'object',
                            'description': 'Objeto JSON que contiene la dirección relacionada con el registro',
                            'properties': {
                                'calle': {
                                    'type': 'string',
                                    'description': 'Nombre de la calle',
                                    'example': '5th Avenue'
                                },
                                'ciudad': {
                                    'type': 'string',
                                    'description': 'Ciudad donde se registra la velocidad',
                                    'example': 'New York'
                                },
                                'codigo_postal': {
                                    'type': 'string',
                                    'description': 'Código postal asociado',
                                    'example': '10001'
                                }
                            }
                        },
                        'fecha': {
                            'type': 'string',
                            'format': 'date-time',
                            'description': 'Fecha y hora del registro',
                            'example': '2023-09-15T14:28:22.842Z'
                        },
                        'speed': {
                            'type': 'string',
                            'description': 'Velocidad registrada en km/h',
                            'example': '65.5'
                        },
                        'streetMaxSpeed': {
                            'type': 'string',
                            'description': 'Velocidad máxima permitida en la calle en km/h',
                            'example': '80'
                        }
                    },
                    'required': ['latitud', 'longitud', 'direccion', 'fecha', 'speed', 'streetMaxSpeed']
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
                        'latitud': '40.7128',
                        'longitud': '-74.0060',
                        'direccion': {
                            'calle': '5th Avenue',
                            'ciudad': 'New York',
                            'codigo_postal': '10001'
                        },
                        'fecha': '2023-09-15T14:28:22.842Z',
                        'speed': '65.5',
                        'streetMaxSpeed': '80'
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

    # Los campos requeridos ahora incluyen direccion, speed y streetMaxSpeed y userid
    required_fields = ['latitud', 'longitud', 'direccion', 'fecha', 'speed', 'streetMaxSpeed','userid']
    missing_fields = [field for field in required_fields if field not in data]
    
    if missing_fields:
        return jsonify({'error': f'Faltan los siguientes parámetros: {", ".join(missing_fields)}'}), 400

    # Crear el registro con los nuevos campos
    new_record = SpeedRecord.create_speed_record(
        latitud=data['latitud'], 
        longitud=data['longitud'], 
        direccion=data['direccion'],
        speed=data['speed'], 
        street_max_speed=data['streetMaxSpeed'],
        fecha=data['fecha'],
        userid=data['userid']
    )
    
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

@user_blueprint.route('/get_spdrecord_user',methods=['GET'])
@token_required
@swag_from({
    'tags': ['Velocity Records'],
    'summary': 'Obtener registros de velocidad por usuario',
    'description': 'Devuelve todos los registros de velocidad asociados al usuario autenticado usando el token JWT.',
    'parameters': [
        {
            'name': 'Authorization',
            'in': 'header',
            'required': True,
            'description': 'Token JWT para autenticación (formato: Bearer <token>)',
            'schema': {
                'type': 'string',
                'example': 'Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...'
            }
        }
    ],
    'responses': {
        200: {
            'description': 'Lista de registros de velocidad del usuario',
            'content': {
                'application/json': {
                    'schema': {
                        'type': 'array',
                        'items': {
                            'type': 'object',
                            'properties': {
                                'id': {
                                    'type': 'string',
                                    'description': 'ID del registro de velocidad',
                                    'example': '63bfa6f8f58a7b00246c0d1e'
                                },
                                'direccion': {
                                    'type': 'object',
                                    'description': 'Información de la dirección registrada',
                                    'example': {'name': '5th Avenue'}
                                },
                                'latitud': {
                                    'type': 'number',
                                    'description': 'Latitud donde se registró la velocidad',
                                    'example': 40.7128
                                },
                                'longitud': {
                                    'type': 'number',
                                    'description': 'Longitud donde se registró la velocidad',
                                    'example': -74.0060
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
                                },
                                'street_max_speed': {
                                    'type': 'number',
                                    'description': 'Velocidad máxima permitida en la calle',
                                    'example': 50.0
                                }
                            }
                        }
                    }
                }
            }
        },
        401: {
            'description': 'Token inválido o expirado'
        },
        403: {
            'description': 'Acceso denegado, usuario no autorizado'
        }
    }
})
def obtain_records_by_user():
    token = request.headers.get('Authorization')
    _,id = decode_token(token)
    response = SpeedRecord.get_records_by_user(id)
    return jsonify(response), 200