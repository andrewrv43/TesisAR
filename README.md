
## 🛠️ Microservicio de Usuarios

![Microservicio de Usuarios](https://img.shields.io/badge/Microservicio-Flask%20%7C%20MongoDB-blue.svg)
![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)
![Python Version](https://img.shields.io/badge/Python-3.8%2B-yellow.svg)

Este microservicio permite gestionar usuarios mediante operaciones CRUD (Crear, Leer, Actualizar, Eliminar). Se conecta a una base de datos MongoDB para almacenar y recuperar la información de los usuarios.

---

### 📋 **Características**

- **Crear usuario:** Añade un nuevo usuario con `nombre`, `contraseña` y `fecha de creación`.
- **Obtener todos los usuarios:** Recupera una lista de todos los usuarios.
- **Obtener un usuario por ID:** Recupera los detalles de un usuario en particular.
- **Actualizar un usuario:** Modifica la información de un usuario específico.
- **Eliminar un usuario:** Elimina a un usuario de la base de datos.

---

### 🚀 **Tecnologías**

- **Flask**: Framework para crear APIs en Python.
- **MongoDB**: Base de datos NoSQL para almacenar la información de los usuarios.
- **Flasgger**: Generador de documentación automática en Swagger para APIs.
- **Docker (Opcional)**: Para contenedores de servicios (si se usa).

---

### 🛠️ **Instalación**

#### **Requisitos previos**

- **Python 3.8+**
- **MongoDB** (Debe estar corriendo en local o en un servidor)
- **Pip** para manejar las dependencias
- (Opcional) **Docker** para ejecutar el microservicio en contenedores.

#### **1. Clonar el repositorio**

```bash
git clone https://github.com/tu-usuario/tu-repositorio.git
cd tu-repositorio/microservices
```

#### **2. Crear y activar un entorno virtual (opcional pero recomendado)**

```bash
python -m venv venv
source venv/bin/activate  # En Linux/Mac
# o
venv\Scripts\activate     # En Windows
```

#### **3. Instalar dependencias**

```bash
pip install -r requirements.txt
```

#### **4. Configurar MongoDB**

Asegúrate de que tienes una instancia de MongoDB corriendo en `localhost:27017` o actualiza la configuración en `app/database.py` si está en otro lugar.

---

### 🏃 **Ejecución**

Para iniciar el microservicio, utiliza el siguiente comando:

```bash
python run.py
```

La API estará disponible en `http://localhost:5000`.

---

### 📖 **Documentación Swagger**

Una vez el servidor esté corriendo, puedes acceder a la documentación interactiva de Swagger para probar los endpoints:

- **URL**: `http://localhost:5000/apidocs/`

---

### 🔗 **Endpoints principales**

#### **1. Crear un nuevo usuario**

- **URL**: `POST /user`
- **Descripción**: Agrega un nuevo usuario.
- **Cuerpo de la solicitud**:

    ```json
    {
      "user": "JohnDoe",
      "pwd": "hashedpassword"
    }
    ```

#### **2. Obtener todos los usuarios**

- **URL**: `GET /user`
- **Descripción**: Devuelve la lista de todos los usuarios registrados.

#### **3. Obtener un usuario por ID**

- **URL**: `GET /user/{id}`
- **Descripción**: Devuelve los detalles de un usuario específico.
- **Parámetros**: 
    - **id**: ID del usuario.

#### **4. Actualizar un usuario**

- **URL**: `PUT /user/{id}`
- **Descripción**: Actualiza la información de un usuario.
- **Cuerpo de la solicitud**:

    ```json
    {
      "user": "JaneDoe",
      "pwd": "newhashedpassword"
    }
    ```

#### **5. Eliminar un usuario**

- **URL**: `DELETE /user/{id}`
- **Descripción**: Elimina un usuario por su ID.

---

### 🐋 **Ejecución con Docker (opcional)**

Si prefieres ejecutar el microservicio en un contenedor Docker:

1. **Construir la imagen de Docker**:

    ```bash
    docker build -t microservice-usuarios .
    ```

2. **Ejecutar el contenedor**:

    ```bash
    docker run -p 5000:5000 microservice-usuarios
    ```

---

### 🧪 **Pruebas**

Para ejecutar las pruebas unitarias:

```bash
pytest tests/
```

---

### 📝 **Licencia**

Este proyecto está licenciado bajo la Licencia MIT. Consulta el archivo [LICENSE](LICENSE) para más detalles.

---

### ✨ **Contribuciones**

¡Las contribuciones son bienvenidas! Si encuentras algún problema o tienes una sugerencia de mejora, por favor crea un **issue** o envía un **pull request**.

---

### 📧 **Contacto**

Para más información o consultas:

- **Email**: contacto@tuservicio.com
- **GitHub**: [@tu-usuario](https://github.com/tu-usuario)

---

### 📂 **Estructura del Proyecto**

```
microservices/
├── app/
│   ├── __init__.py
│   ├── routes/
│   ├── models/
│   ├── database.py
├── docs/
│   └── swagger_config.yml
├── run.py
├── requirements.txt
└── README.md
```

### By Andrew R.
