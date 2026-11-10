package com.trabajaya.events;
import java.util.*;
public record DomainEvent(String id,String kind,Long actorId,Long jobId,String title,String detail,List<Long> recipients) {
    public static DomainEvent of(String kind,Long actor,Long job,String title,String detail,List<Long> recipients){
        return new DomainEvent(UUID.randomUUID().toString(),kind,actor,job,title,detail,List.copyOf(recipients));
    }
}
