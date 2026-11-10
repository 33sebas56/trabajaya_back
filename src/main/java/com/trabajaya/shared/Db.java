package com.trabajaya.shared;
import java.util.*;
import java.time.*;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
@Component
public class Db {
    private final NamedParameterJdbcTemplate jdbc;
    public Db(NamedParameterJdbcTemplate jdbc){
        this.jdbc=jdbc;
    }
    public static Map<String,Object> p(Object... pairs){
        var m=new HashMap<String,Object>();
        for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);
        return m;
    }
    public List<Map<String,Object>> list(String sql,Map<String,?> p){
        return jdbc.queryForList(sql,p);
    }
    public List<Map<String,Object>> list(String sql){
        return list(sql,Map.of());
    }
    public Optional<Map<String,Object>> one(String sql,Map<String,?> p){
        return list(sql,p).stream().findFirst();
    }
    public Map<String,Object> require(String sql,Map<String,?> p){
        return one(sql,p).orElseThrow(ApiException::missing);
    }
    public int update(String sql,Map<String,?> p){
        return jdbc.update(sql,p);
    }
    public long insert(String sql,Map<String,?> p){
        var key=new GeneratedKeyHolder();
        jdbc.update(sql,new MapSqlParameterSource(p),key,new String[]{
            "id"
        }
        );
        return Objects.requireNonNull(key.getKey()).longValue();
    }
    public long count(String sql,Map<String,?> p){
        return Objects.requireNonNull(jdbc.queryForObject(sql,p,Long.class));
    }
    public static long id(Map<String,?> m,String k){
        return ((Number)m.get(k)).longValue();
    }
    public static String text(Map<String,?> m,String k){
        return Objects.toString(m.get(k),"");
    }
    public static boolean bool(Map<String,?> m,String k){
        return Boolean.TRUE.equals(m.get(k));
    }
    public static OffsetDateTime time(Map<String,?> m,String k){
        Object v=m.get(k);
        if(v==null)return null;
        if(v instanceof OffsetDateTime t)return t;
        if(v instanceof java.sql.Timestamp t)return t.toInstant().atOffset(ZoneOffset.UTC);
        return OffsetDateTime.parse(v.toString());
    }
    public static Double decimal(Map<String,?> m,String k){
        return m.get(k)==null?null:((Number)m.get(k)).doubleValue();
    }
}
