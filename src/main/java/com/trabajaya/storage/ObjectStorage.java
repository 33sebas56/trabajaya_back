package com.trabajaya.storage;
public interface ObjectStorage {
    String provider();
    void write(String path,byte[] bytes,String contentType);
    byte[] read(String path);
    void delete(String path);
}
