import com.trabajaya.jobs.JobRules;
import java.util.List;
public class DomainSmoke {
    static int checks=0;
    static void check(boolean ok,String label){
        checks++;
        if(!ok)throw new AssertionError(label);
    }
    public static void main(String[] args){
        check(JobRules.increasedPrice(60000,10000,100000)==70000,"Increment");
        check(JobRules.increasedPrice(95000,20000,100000)==100000,"Ceiling");
        check(JobRules.increasedPrice(Long.MAX_VALUE-2,100,Long.MAX_VALUE)==Long.MAX_VALUE,"Overflow");
        check(JobRules.canAdvance("ACEPTADO","EN_CAMINO"),"First state");
        check(!JobRules.canAdvance("ACEPTADO","TERMINADO"),"Cannot skip states");
        check(!JobRules.canAdvance("CONFIRMADO","ACEPTADO"),"Terminal state");
        check(JobRules.aggregate(List.of("ACEPTADO"),2).equals("PUBLICADO"),"Partial slots");
        check(JobRules.aggregate(List.of("ACEPTADO","EN_CAMINO"),2).equals("ASIGNADO"),"Full slots");
        check(JobRules.aggregate(List.of("INICIADO","EN_CAMINO"),2).equals("EN_CURSO"),"Execution");
        check(JobRules.aggregate(List.of("CONFIRMADO","TERMINADO"),2).equals("POR_CONFIRMAR"),"Confirmation");
        check(JobRules.aggregate(List.of("CONFIRMADO","CONFIRMADO"),2).equals("COMPLETADO"),"Completed");
        check(JobRules.aggregate(List.of("CANCELADO"),1).equals("PUBLICADO"),"Released slot");
        check(!JobRules.cancellable("INICIADO"),"No unilateral cancellation while executing");
        boolean rejected=false;
        try{
            JobRules.increasedPrice(5000,-1,9000);
        }
        catch(IllegalArgumentException expected){
            rejected=true;
        }
        check(rejected,"Bad amount");
        System.out.println("PASS: "+checks+" pure Java domain checks.");
    }
}
