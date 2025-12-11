package com.ktb.chatapp.websocket.socketio.handler;

import com.ktb.chatapp.config.MongoTestContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(MongoTestContainer.class)
@TestPropertySource(properties = {
        "spring.data.mongodb.auto-index-creation=true",
        "socketio.enabled=false"
})
class MessageLoaderIntegrationTest {


}
