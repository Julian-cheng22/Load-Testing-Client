package edu.neu.cs6650.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
public class ServerApp {

    @PostMapping("/products")   // 或者 "/api/products"，看你 client 怎么写
    public ResponseEntity<Void> receive(@RequestBody String body) {
        // 不做任何处理，只是假装创建成功
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public static void main(String[] args) {
        SpringApplication.run(ServerApp.class, args);
    }
}