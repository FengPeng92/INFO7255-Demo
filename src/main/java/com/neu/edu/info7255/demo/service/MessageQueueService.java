package com.neu.edu.info7255.demo.service;

import com.neu.edu.info7255.demo.dao.MessageQueueDao;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageQueueService {

    @Autowired
    private MessageQueueDao messageQueueDao;

    public void addToMessageQueue(String message, boolean isDelete) {
        JSONObject object = new JSONObject();
        object.put("message", message);
        object.put("isDelete", isDelete);

        // save plan to message queue "messageQueue"
        messageQueueDao.addToQueue("messageQueue", object.toString());
    }
}
