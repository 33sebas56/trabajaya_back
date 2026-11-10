package com.trabajaya.shared;
public record Principal(long id,String role,String name) {
    public boolean is(String expected){
        return role.equals(expected);
    }
    public void require(String expected){
        if(!is(expected))throw ApiException.forbidden();
    }
}
