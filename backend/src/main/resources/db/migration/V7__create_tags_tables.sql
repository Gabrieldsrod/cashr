CREATE TABLE tags (
    id      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID         NOT NULL,
    name    VARCHAR(50)  NOT NULL,

    CONSTRAINT fk_tags_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_tags_user_name UNIQUE (user_id, name)
);

CREATE INDEX idx_tags_user_id_name ON tags(user_id, name);

CREATE TABLE transaction_tags (
    transaction_id UUID NOT NULL,
    tag_id         UUID NOT NULL,

    CONSTRAINT pk_transaction_tags        PRIMARY KEY (transaction_id, tag_id),
    CONSTRAINT fk_transaction_tags_tx     FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_tags_tag    FOREIGN KEY (tag_id)         REFERENCES tags(id)         ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_transaction_tags ON transaction_tags(transaction_id, tag_id);
CREATE INDEX idx_transaction_tags_tag_id ON transaction_tags(tag_id);
