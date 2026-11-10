package com.trabajaya;
import com.trabajaya.jobs.JobRules;
import com.trabajaya.shared.Geo;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class JobRulesTest {
    @Test void priceNeverExceedsCeiling(){
        assertEquals(100_000,JobRules.increasedPrice(95_000,20_000,100_000));
        assertEquals(70_000,JobRules.increasedPrice(60_000,10_000,100_000));
        assertThrows(IllegalArgumentException.class,()->JobRules.increasedPrice(60_000,-1,100_000));
    }
    @Test void priceDoesNotOverflow(){
        assertEquals(Long.MAX_VALUE,JobRules.increasedPrice(Long.MAX_VALUE-2,100,Long.MAX_VALUE));
    }
    @Test void cannotSkipExecutionStates(){
        assertTrue(JobRules.canAdvance("ACEPTADO","EN_CAMINO"));
        assertFalse(JobRules.canAdvance("ACEPTADO","TERMINADO"));
        assertFalse(JobRules.canAdvance("CONFIRMADO","ACEPTADO"));
    }
    @Test void groupCompletionNeedsEveryWorker(){
        assertEquals("PUBLICADO",JobRules.aggregate(List.of("ACEPTADO"),2));
        assertEquals("ASIGNADO",JobRules.aggregate(List.of("ACEPTADO","EN_CAMINO"),2));
        assertEquals("POR_CONFIRMAR",JobRules.aggregate(List.of("TERMINADO","CONFIRMADO"),2));
        assertEquals("COMPLETADO",JobRules.aggregate(List.of("CONFIRMADO","CONFIRMADO"),2));
        assertEquals("EN_CURSO",JobRules.aggregate(List.of("INICIADO","ACEPTADO"),2));
        assertEquals("PUBLICADO",JobRules.aggregate(List.of("CANCELADO"),1));
    }
    @Test void distanceAndValidation(){
        assertEquals(0,Geo.km(1.2,-77.2,1.2,-77.2),0.001);
        assertEquals(111.195,Geo.km(0,0,1,0),0.1);
        assertThrows(RuntimeException.class,()->Geo.validate(91.0,0.0));
        assertThrows(RuntimeException.class,()->Geo.validate(0.0,null));
        Geo.validate(null,null);
    }
}
