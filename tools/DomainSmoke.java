import com.trabajaya.jobs.JobRules;
import com.trabajaya.events.DomainEvent;
import com.trabajaya.shared.PageWindow;
import com.trabajaya.storage.ObjectKey;
import com.trabajaya.users.FixedWindowRateLimiter;
import com.trabajaya.users.PasswordPolicy;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Executable checks for dependency-free rules. Not a substitute for Spring integration tests. */
public class DomainSmoke {
    private static int checks;

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void rejects(Runnable action, String label) {
        boolean rejected = false;
        try { action.run(); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, label);
    }

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }

    public static void main(String[] args) throws Exception {
        check(JobRules.increasedPrice(60000,10000,100000)==70000, "Increment");
        check(JobRules.increasedPrice(95000,20000,100000)==100000, "Ceiling");
        check(JobRules.increasedPrice(Long.MAX_VALUE-2,100,Long.MAX_VALUE)==Long.MAX_VALUE, "Overflow");
        check(JobRules.increasedPrice(100000,10000,100000)==100000, "Ceiling reached");
        rejects(() -> JobRules.increasedPrice(5000,-1,9000), "Negative increment");
        rejects(() -> JobRules.increasedPrice(0,1,9000), "Zero initial");
        rejects(() -> JobRules.increasedPrice(10000,1,9000), "Ceiling below current");
        for (String[] step : new String[][] {{"ACEPTADO","EN_CAMINO"},{"EN_CAMINO","EN_LUGAR"},
                {"EN_LUGAR","INICIADO"},{"INICIADO","TERMINADO"}}) {
            check(JobRules.canAdvance(step[0],step[1]), "Forward state");
        }
        check(!JobRules.canAdvance("ACEPTADO","TERMINADO"), "Cannot skip states");
        check(!JobRules.canAdvance("CONFIRMADO","ACEPTADO"), "Terminal state");
        check(!JobRules.canAdvance(null,"ACEPTADO"), "Missing current state");
        check(!JobRules.canAdvance("ACEPTADO",null), "Missing target state");
        check(JobRules.aggregate(List.of("ACEPTADO"),2).equals("PUBLICADO"), "Partial slots");
        check(JobRules.aggregate(List.of("ACEPTADO","EN_CAMINO"),2).equals("ASIGNADO"), "Full slots");
        check(JobRules.aggregate(List.of("INICIADO","EN_CAMINO"),2).equals("EN_CURSO"), "Execution");
        check(JobRules.aggregate(List.of("CONFIRMADO","TERMINADO"),2).equals("POR_CONFIRMAR"), "Confirmation");
        check(JobRules.aggregate(List.of("CONFIRMADO","CONFIRMADO"),2).equals("COMPLETADO"), "Completed");
        check(JobRules.aggregate(List.of("CANCELADO"),1).equals("PUBLICADO"), "Released slot");
        check(JobRules.aggregate(List.of(),1).equals("PUBLICADO"), "No assigned workers");
        rejects(() -> JobRules.aggregate(List.of(),0), "Invalid required count");
        for (String state : List.of("ACEPTADO","EN_CAMINO","EN_LUGAR")) check(JobRules.cancellable(state), "Allowed cancellation");
        for (String state : List.of("INICIADO","TERMINADO","CONFIRMADO")) check(!JobRules.cancellable(state), "Cancellation blocked");

        check(PasswordPolicy.valid("Prueba2026!"), "Valid password");
        check(!PasswordPolicy.valid(null), "Missing password");
        check(!PasswordPolicy.valid("a1"), "Short password");
        check(!PasswordPolicy.valid("password"), "No digit");
        check(!PasswordPolicy.valid("12345678"), "No letter");
        check(PasswordPolicy.valid("A1"+"x".repeat(70)), "72 bytes");
        check(!PasswordPolicy.valid("A1"+"x".repeat(71)), "73 bytes");
        check(!PasswordPolicy.valid("A1"+"\u00e9".repeat(36)), "UTF-8 byte bound");
        check(PasswordPolicy.validAdministrator("Administrador2026!"), "Admin password");
        check(!PasswordPolicy.validAdministrator("Prueba12"), "Short admin password");
        check(PageWindow.offset(-1,20)==0, "Negative page");
        check(PageWindow.offset(2,20)==40, "Normal page");
        check(PageWindow.offset(Integer.MAX_VALUE,50)==107374182350L, "Long offset");
        rejects(() -> PageWindow.offset(1,0), "Invalid page size");

        String uuid="550e8400-e29b-41d4-a716-446655440000", key="12/"+uuid+".png";
        check(ObjectKey.create(12,uuid).equals(key), "Object key generated");
        check(ObjectKey.require(key).equals(key), "Object key accepted");
        rejects(() -> ObjectKey.create(0,uuid), "Bad owner");
        for (String invalid : List.of("../secret", "/etc/passwd", "https://example.com/a", "1/a.png", key+"?q=1", "12/../"+uuid+".png")) {
            rejects(() -> ObjectKey.require(invalid), "Unsafe object key");
        }
        rejects(() -> ObjectKey.require(null), "Null object key");

        var clock = new MutableClock();
        var limiter = new FixedWindowRateLimiter(clock, Duration.ofMinutes(1), 2);
        check(limiter.allow("a",2), "First request");
        check(limiter.allow("a",2), "Second request");
        check(!limiter.allow("a",2), "Excess request");
        check(limiter.allow("b",2), "Second key");
        check(!limiter.allow("c",2), "Capacity bound");
        check(limiter.size()==2, "Memory bound");
        clock.now=clock.now.plusSeconds(60);
        check(limiter.allow("c",2), "Expired capacity released");
        check(limiter.allow("a",2), "Expired window");
        rejects(() -> new FixedWindowRateLimiter(clock,Duration.ZERO,2), "Invalid duration");
        var concurrent = new FixedWindowRateLimiter(Clock.systemUTC(),Duration.ofMinutes(1),10);
        try (var pool=Executors.newFixedThreadPool(8)) {
            var work = new ArrayList<Callable<Boolean>>();
            for (int i=0;i<100;i++) work.add(() -> concurrent.allow("same",7));
            int allowed=0;
            for (var result : pool.invokeAll(work)) if (result.get()) allowed++;
            check(allowed==7, "Concurrent requests respect limit");
        }
        var recipients=new ArrayList<>(List.of(1L,2L));
        var event=DomainEvent.of("JOB_PUBLISHED",null,1L,"Title","Detail",recipients);
        recipients.clear();
        check(event.recipients().equals(List.of(1L,2L)), "Event snapshot");
        check(event.actorId()==null, "Scheduler event may have no actor");
        boolean immutable=false;
        try { event.recipients().add(3L); } catch (UnsupportedOperationException expected) { immutable=true; }
        check(immutable, "Immutable recipients");
        System.out.println("PASS: "+checks+" dependency-free Java checks. Spring, SQL and cloud services NOT covered by this command.");
    }
}
