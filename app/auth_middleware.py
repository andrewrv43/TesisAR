import jwt
from flask import request, jsonify, current_app
from functools import wraps

def token_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        token = request.headers.get('Authorization')
        user = 'No token'

        if token:
            try:
                token = token.split()[1] if "Bearer" in token else token
                decoded_token = jwt.decode(token, current_app.config['SECRET_KEY'], algorithms=['HS256'])
                user = decoded_token.get('user', 'Unknown')

            except jwt.ExpiredSignatureError:
                current_app.logger.info(f"User: Invalid token (expired) accessed endpoint: {request.path} from {request.remote_addr}")
                return jsonify({'message': 'Token has expired!'}), 403
            except jwt.InvalidTokenError:
                current_app.logger.info(f"User: Invalid token accessed endpoint: {request.path} from {request.remote_addr}")
                return jsonify({'message': 'Invalid token!'}), 403
        else:
            current_app.logger.info(f"User: No token accessed endpoint: {request.path} from {request.remote_addr}")
            return jsonify({'message': 'Token is missing!'}), 403

        current_app.logger.info(f"User: {user} accessed endpoint: {request.path} from {request.remote_addr}")
        return f(*args, **kwargs)

    return decorated
