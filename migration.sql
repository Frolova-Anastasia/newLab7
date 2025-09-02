
ALTER TABLE products ADD COLUMN position_index INTEGER;


WITH numbered AS (
  SELECT id, ROW_NUMBER() OVER (ORDER BY creation_date, id) AS rn
  FROM products
)
UPDATE products
SET position_index = numbered.rn
FROM numbered
WHERE products.id = numbered.id;

ALTER TABLE products ALTER COLUMN position_index SET NOT NULL;

CREATE INDEX idx_products_position_index ON products(position_index);
