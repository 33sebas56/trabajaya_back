package com.trabajaya.jobs;
import java.util.*;
/** Pure domain rules. No framework or database dependencies. */
public final class JobRules {
    private JobRules(){
    }
    private static final Map<String,String> NEXT=Map.of("ACEPTADO","EN_CAMINO","EN_CAMINO","EN_LUGAR","EN_LUGAR","INICIADO","INICIADO","TERMINADO");
    public static boolean canAdvance(String current,String next){
        return next.equals(NEXT.get(current));
    }
    public static long increasedPrice(long current,long step,long ceiling){
        if(current<=0||step<=0||ceiling<current)throw new IllegalArgumentException("Configuracion de precio invalida");
        return current+Math.min(step,ceiling-current);
    }
    public static String aggregate(List<String> states,int needed){
        var active=states.stream().filter(s->!s.equals("CANCELADO")).toList();
        if(active.size()==needed&&active.stream().allMatch(s->s.equals("CONFIRMADO")))return "COMPLETADO";
        if(active.size()==needed&&active.stream().allMatch(s->s.equals("TERMINADO")||s.equals("CONFIRMADO")))return "POR_CONFIRMAR";
        if(active.stream().anyMatch(s->Set.of("INICIADO","TERMINADO","CONFIRMADO").contains(s)))return "EN_CURSO";
        return active.size()>=needed?"ASIGNADO":"PUBLICADO";
    }
    public static boolean cancellable(String status){
        return Set.of("ACEPTADO","EN_CAMINO","EN_LUGAR").contains(status);
    }
}
