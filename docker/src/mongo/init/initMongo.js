db = db.getSiblingDB('file_management');
db.createUser({
    user: "username",
    pwd: "password",
    roles: [{role: "readWrite", db: "file_management"}]
});
db.createCollection('dummy_collection');