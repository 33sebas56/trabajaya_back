package com.trabajaya.shared;
public final class Geo {
    private Geo(){
    }
    public static void validate(Double lat,Double lon){
        if((lat==null)!=(lon==null))throw ApiException.bad("Indica ambas coordenadas o ninguna.");
        if(lat!=null&&(!Double.isFinite(lat)||!Double.isFinite(lon)||lat < -90||lat>90||lon < -180||lon>180))throw ApiException.bad("Coordenadas invalidas.");
    }
    public static double km(double a,double b,double c,double d){
        double p=Math.toRadians(c-a),q=Math.toRadians(d-b);
        double x=Math.sin(p/2)*Math.sin(p/2)+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(c))*Math.sin(q/2)*Math.sin(q/2);
        return 6371*2*Math.atan2(Math.sqrt(x),Math.sqrt(Math.max(0,1-x)));
    }
}
