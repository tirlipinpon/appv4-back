-- MVP hello : messages smoke pour learning (front_) et admin (back_)

CREATE TABLE front_hello_messages (
    id    BIGSERIAL PRIMARY KEY,
    text  VARCHAR(255) NOT NULL
);

CREATE TABLE back_hello_messages (
    id    BIGSERIAL PRIMARY KEY,
    text  VARCHAR(255) NOT NULL
);

INSERT INTO front_hello_messages (text) VALUES ('hello learning');
INSERT INTO back_hello_messages (text) VALUES ('admin-helloworld');
