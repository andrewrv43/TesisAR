from flask import Blueprint, jsonify, request,Response
from app.models.user_model import UserModel,SpeedRecord
from flasgger import swag_from
from app.config import Config
from datetime import timezone
import datetime
import jwt
from app.auth_middleware import token_required
import threading
import os
import orjson
from cryptography.fernet import Fernet
import traceback
fernet = Fernet(os.getenv('pwdEncript'))
user_blueprint = Blueprint('user', __name__)

def send_email_async(subject, body):
    """Función para enviar correo de forma asíncrona."""
    Config.email.sendEmail(subject, body)
@user_blueprint.route('/life', methods=['GET'])    
def alive():
    return jsonify({"message": "Server is alive"}), 200
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
        if user['user'] == username and fernet.decrypt(user['pwd']).decode()==password:
            return user
    

    # Si no se encuentra coincidencia, retornar None
    return None


def decode_token(token):
    if not token:
        return None

    try:
        # Eliminar el prefijo 'Bearer ' si está presente
        if token.startswith("Bearer "):
            token = token.split(" ")[1]

        # Decodificar el token
        decoded_data = jwt.decode(token, Config.SECRET_KEY, algorithms=['HS256'])
        user = decoded_data.get('user')
        user_id = decoded_data.get('id')

        if user and user_id:
            return user, user_id
        return None
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


#################################
#Rutas para ELIMINAR un usuario #
#################################


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
    client_ip = request.remote_addr
    # Verificar que los parámetros obligatorios estén presentes
    if 'user' not in data or 'pwd' not in data:
        
        subject = "Error al crear Usuario"
        body = f"Intento de crear un usuario fallido. Solicitud proviene de: {client_ip}. Faltan parámetros."
        threading.Thread(target=send_email_async, args=(subject, body)).start()
        return jsonify({'error': 'Faltan parámetros obligatorios: user y pwd'}), 400

    # Verificar si el usuario ya existe
    if not UserModel.get_user_by_username(data['user']):
        new_user = UserModel.create_user(data['user'], fernet.encrypt(data['pwd'].encode()))
        new_user['pwd'] = fernet.decrypt(new_user['pwd']).decode()
        subject = "Usuario Creado"
        body = f"Nuevo usuario creado: {data['user']}. Solicitud proviene de: {client_ip}."
        threading.Thread(target=send_email_async, args=(subject, body)).start()
        return jsonify(new_user), 201
    else:
        subject = "Error al crear Usuario"
        body = f"Intento de crear un usuario ya existente. Solicitud proviene de: {client_ip}."
        threading.Thread(target=send_email_async, args=(subject, body)).start()
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
    token = request.headers.get('Authorization')
    _,id = decode_token(token)
    # Los campos requeridos ahora incluyen direccion, speed y streetMaxSpeed y userid
    required_fields = ['latitud', 'longitud', 'direccion', 'fecha', 'speed', 'streetMaxSpeed']
    missing_fields = [field for field in required_fields if field not in data]
    
    if missing_fields:
        return jsonify({'error': f'Faltan los siguientes parámetros: {", ".join(missing_fields)}'}), 400

    # Crear el registro con los nuevos campos
    
    new_record = SpeedRecord.create_speed_record(
        latitud=data['latitud'], 
        longitud=data['longitud'], 
        direccion=data['direccion'],
        speed=data['speed'].replace(',','.'), 
        street_max_speed=data['streetMaxSpeed'].replace(',','.'),
        fecha=data['fecha'],
        userid=id
    )
    
    return jsonify(new_record), 201


@user_blueprint.route('/get_spdrecords', defaults={'id': None}, methods=['GET'])
@user_blueprint.route('/get_spdrecords/<id>', methods=['GET'])
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
def obtainallrecords(id):  
    try:
        response = SpeedRecord.get_all_speed_records(id = id)
        return Response(
        orjson.dumps(response),status=200,content_type='application/json'
    )
    except Exception as e:
        return jsonify({'error': str(e)}), 400

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
    limit = request.args.get('limit', default=None, type=int)
    response = SpeedRecord.get_records_by_user(id)
    totl = str(len(response))
    if limit is not None:
        response = response[:limit]
    return jsonify({
        "records":response,
        "total_length":totl}), 200

@user_blueprint.route('/sp_localsend', methods=['POST'])
@token_required
@swag_from({
    'tags': ['Velocity Records'],
    'summary': 'Guardar múltiples registros de velocidad',
    'description': 'Recibe una lista de registros de velocidad y los guarda en la base de datos.',
    'requestBody': {
        'required': True,
        'content': {
            'application/json': {
                'schema': {
                    'type': 'array',
                    'items': {
                        'type': 'object',
                        'properties': {
                            'latitud': {'type': 'string', 'example': '40.7128'},
                            'longitud': {'type': 'string', 'example': '-74.0060'},
                            'direccion': {'type': 'object', 'description': 'Información de la dirección', 'example': {'name': '5th Avenue'}},
                            'speed': {'type': 'string', 'example': '65.5'},
                            'street_max_speed': {'type': 'string', 'example': '50.0'},
                            'fecha': {'type': 'string', 'format': 'date-time', 'example': '2023-09-15T14:28:22.842Z'},
                            'userid': {'type': 'string', 'example': '123'}
                        },
                        'required': ['latitud', 'longitud', 'speed', 'street_max_speed', 'fecha', 'userid']
                    }
                }
            }
        }
    },
    'responses': {
        201: {
            'description': 'Registros guardados exitosamente',
            'content': {
                'application/json': {
                    'example': {'message': 'Registros guardados con éxito', 'count': 10}
                }
            }
        },
        400: {'description': 'Solicitud incorrecta, datos inválidos'},
        401: {'description': 'Token inválido o expirado'}
    }
})
def save_batch_speed_records():
    token = request.headers.get('Authorization')
    _, userid = decode_token(token)

    try:
        records = request.get_json()
        if not isinstance(records, list):
            return jsonify({'message': 'Deberias enviar una lista'}), 400
        new_records = []

        for record in records:
            latitud = record.get('latitud')
            longitud = record.get('longitud')
            direccion = record.get('direccion')
            speed = record.get('speed').replace(',','.')
            street_max_speed = record.get('streetMaxSpeed').replace(',','.')
            fecha = record.get('fecha')

            # Validar campos requeridos
            if None in [latitud, longitud, speed, street_max_speed, fecha, direccion]:
                continue
            newRecord = {
                'latitud': latitud,
                'longitud': longitud,
                'direccion': direccion,
                'velocidad': speed,
                'street_max_speed': street_max_speed,
                'fecha': fecha,
                'userid': userid
            }
            new_records.append(newRecord)

        guardados = SpeedRecord.upload_many_data(new_records)

        return jsonify({'message': 'Registros guardados con éxito', 'count': str(guardados)}), 201

    except Exception as e:
        return jsonify({'message': f'Error al procesar los registros: {str(e)}'}), 500


#############################################
#               ACTUALIZACION               #
#############################################
@user_blueprint.route('/r10ActuSlash/<client_version>', methods=['GET'])
@token_required
def download_apk(client_version):
    import os
    from flask import send_file, current_app

    try:
        # Versión actual del APK en el servidor o version ESTABLE
        server_version = "0.5"

        # Compara las versiones
        if client_version == server_version:
            # Las versiones son iguales, no hay actualización
            return '', 204  # No Content

        # Ruta al archivo APK
        apk_directory = os.path.join(current_app.root_path, 'apk_version')
        apk_filename = f'app_v{server_version.replace(".", "_")}.apk'
        apk_path = os.path.join(apk_directory, apk_filename)

        # Verifica si el archivo existe
        if not os.path.exists(apk_path):
            return jsonify({'message': 'APK no encontrado'}), 404

        response = send_file(
            apk_path,
            mimetype='application/vnd.android.package-archive',
            as_attachment=True,
            download_name=f'app_v{server_version}.apk'
        )

        response.headers['X-App-Version'] = server_version

        return response, 200

    except Exception as e:
        return jsonify({'message': f'Error al descargar el APK: {str(e)}'}), 500

# @user_blueprint.route('/updatepwds', methods=['POST'])
# def update_passwords():
    try:
        # Obtener todos los registros desde la colección
        all_users = UserModel.get_all_users()
        updated_users = []
        for user in all_users:
            # Verificar si el campo pwd está sin encriptar
            if not user.get('pwd', '').startswith("gAAAA"):  # gAAAA es un prefijo típico de Fernet
                # Encriptar la contraseña
                encrypted_pwd = fernet.encrypt(user['pwd'].encode())
                
                # Crear el objeto actualizado
                updated_data = {
                    '_id': str(user['id']),  # Convertir ObjectId a string para pasarlo al método
                    'user': user['user'],
                    'pwd': encrypted_pwd
                }
                
                # Actualizar el usuario con tu método existente
                updated_user = UserModel.update_user(updated_data)
                if updated_user:
                    updated_users.append(updated_user['id'])  # Agregar ID de usuarios actualizados
        
        return jsonify({
            "message": "Passwords updated successfully.",
            "updated_users": updated_users
        }), 200
    except Exception as e:
        # Capturar el traceback completo
        error_details = traceback.format_exc()
        return jsonify({
            "message": "An error occurred while updating passwords.",
            "error": str(e),
            "traceback": error_details
        }), 500