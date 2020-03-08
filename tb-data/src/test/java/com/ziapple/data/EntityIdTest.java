package com.ziapple.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziapple.server.data.EntityType;
import com.ziapple.server.data.id.DeviceId;
import com.ziapple.server.data.id.EntityIdFactory;
import org.junit.Test;

import java.util.UUID;

public class EntityIdTest {
    @Test
    public void testEntityIdJson(){
        DeviceId deviceId = (DeviceId) EntityIdFactory.getByTypeAndUuid(EntityType.DEVICE, UUID.randomUUID());
        ObjectMapper objectMapper = new ObjectMapper();
        String json = null;
        try {
            json = objectMapper.writeValueAsString(deviceId);
            System.out.println(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
