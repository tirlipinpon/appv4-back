package com.appv4.hello.infrastructure.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "front_hello_messages")
class FrontHelloMessageEntity extends HelloMessageEntityBase {
}
