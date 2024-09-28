from flask import Blueprint, jsonify, request
from app.models.user_model import UserModel
from flasgger import swag_from

user_blueprint = Blueprint('user', __name__)

# Ruta para obtener todos los usuarios
@user_blueprint.route('/user', methods=['GET'])
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

# Ruta para crear un nuevo usuario
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
def create_new_user():  # Cambiado de create_user a create_new_user
    data = request.get_json()
    new_user = UserModel.create_user(data['user'], data['pwd'])
    return jsonify(new_user), 201
# Ruta para actualizar un usuario por ID
@user_blueprint.route('/user/<id>', methods=['PUT'])
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
def update_user(id):
    data = request.get_json()
    updated_user = UserModel.update_user(id, data)
    if updated_user:
        return jsonify(updated_user), 200
    else:
        return jsonify({'error': 'Usuario no encontrado'}), 404

# Ruta para eliminar un usuario
@user_blueprint.route('/user/<id>', methods=['DELETE'])
@swag_from({
    'tags': ['USER'],
    'summary': 'Eliminar un usuario por ID',
    'description': 'Elimina un usuario registrado a partir de su ID',
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
            'description': 'Usuario eliminado exitosamente'
        },
        404: {
            'description': 'Usuario no encontrado'
        }
    }
})
def delete_user(id):
    deleted = UserModel.delete_user(id)
    if deleted:
        return jsonify({'message': 'Usuario eliminado exitosamente'}), 200
    else:
        return jsonify({'error': 'Usuario no encontrado'}), 404
