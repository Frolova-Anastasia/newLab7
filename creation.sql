CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL
);

CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    x INTEGER NOT NULL,
    y BIGINT NOT NULL,
    price REAL CHECK (price > 0),
    unit_of_measure TEXT,
    creation_date TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id INTEGER REFERENCES users(id)
);

CREATE TABLE organizations (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    full_name TEXT,
    type TEXT,
    product_id INTEGER REFERENCES products(id)
);
