package com.appv4.hello.infrastructure.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "back_hello_messages")
class BackHelloMessageEntity extends HelloMessageEntityBase {
}
