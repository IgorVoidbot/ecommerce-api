CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price NUMERIC(10, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL,
    category_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE INDEX idx_products_category_id ON products (category_id);

INSERT INTO users (name, email, password, role)
SELECT 'Admin', 'admin@ecommerce.com', '$2b$10$KsnfWHW.PEOY1ZQT4f7.LOGSTzNOaRxr5lSvA8ZPTuNGEj10r4Y3u', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@ecommerce.com');

INSERT INTO categories (name)
SELECT v.name
FROM (VALUES ('Programação'), ('Mobile'), ('Front-end'), ('Infraestrutura'), ('Business'), ('Design e UX')) AS v(name)
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.name = v.name);
