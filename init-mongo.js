db = db.getSiblingDB('admin');
db.createUser({
    user: process.env.MONGO_INITDB_ROOT_USERNAME,
    pwd: process.env.MONGO_INITDB_ROOT_PASSWORD,
    roles: [ { role: "root", db: "admin" } ]
});
db = db.getSiblingDB('sp_base');
db.createUser({
    user: process.env.MONGO_INITDB_ROOT_USERNAME,
    pwd: process.env.MONGO_INITDB_ROOT_PASSWORD,
    roles: [ { role: "root", db: "admin" } ]
});
db.createCollection('sp_users');
db.createCollection('sp_record');
db.createCollection('sp_backup');
