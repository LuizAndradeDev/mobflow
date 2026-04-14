package com.mobflow.notificationservice.repository;

import com.mobflow.notificationservice.model.entities.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

}
