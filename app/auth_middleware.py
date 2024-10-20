import jwt
from flask import request, jsonify, current_app
from functools import wraps

def token_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        token = request.headers.get('Authorization')
        user = 'No token'

        secret_key = current_app.config.get('SECRET_KEY')
        if not isinstance(secret_key, str):
            return jsonify({'message': 'Invalid secret key format.'}), 500

        if token:
            try:
                token = token.split()[1] if "Bearer" in token else token
                decoded_token = jwt.decode(token, secret_key, algorithms=['HS256'])
                user = decoded_token.get('user', 'Unknown')

            except jwt.ExpiredSignatureError:
                return jsonify({'message': 'Token has expired!'}), 403
            except jwt.InvalidTokenError:
                return jsonify({'message': 'Invalid token!'}), 403
            except TypeError as e:
                return jsonify({'message': str(e)}), 400
        else:
            return jsonify({'message': 'Token is missing!'}), 403

        return f(*args, **kwargs)

    return decorated
