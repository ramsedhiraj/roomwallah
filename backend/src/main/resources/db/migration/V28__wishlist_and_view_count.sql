CREATE TABLE wishlist_items (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    property_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_wishlist_items_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_items_property FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    CONSTRAINT uq_wishlist_user_property UNIQUE (user_id, property_id)
);

CREATE INDEX idx_wishlist_items_user ON wishlist_items(user_id);
CREATE INDEX idx_wishlist_items_property ON wishlist_items(property_id);

ALTER TABLE properties ADD COLUMN view_count INT NOT NULL DEFAULT 0;
ALTER TABLE search_documents ADD COLUMN view_count INT NOT NULL DEFAULT 0;

CREATE INDEX idx_properties_view_count ON properties(view_count DESC);
CREATE INDEX idx_search_documents_view_count ON search_documents(view_count DESC);
