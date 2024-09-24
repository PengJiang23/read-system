package com.heima.wemedia;

import org.apache.kafka.clients.KafkaClient;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Properties;

@SpringBootTest(classes = WemediaApplication.class)
@RunWith(SpringRunner.class)
public class CommonTest {

    @Test
    public void test() {
        String originalFilename = "1.mp4";
        System.out.println(originalFilename.lastIndexOf("."));

        String postfix = originalFilename.substring(originalFilename.lastIndexOf(".")+1);
        System.out.println(postfix);
    }


    @Value("${spring.kafka.bootstrap-servers}")
    String hostip;

    @Test
    public void mqTest(){

        System.out.println(hostip);



    }



}
