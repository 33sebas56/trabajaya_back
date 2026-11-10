package com.trabajaya.jobs;
import com.trabajaya.shared.Principal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/api")
public class JobController {
    private final JobService service;
    public JobController(JobService service){
        this.service=service;
    }
    @GetMapping("/jobs") public Object list(@AuthenticationPrincipal Principal p,@RequestParam(defaultValue="false")boolean mine,@RequestParam(required=false)Long category,@RequestParam(required=false)Long minimum,@RequestParam(required=false)String city,@RequestParam(required=false)String date,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){
        return service.list(p,mine,category,minimum,city,date,page,size);
    }
    @GetMapping("/jobs/{id}") public Object detail(@PathVariable long id,@AuthenticationPrincipal Principal p){
        return service.detail(id,p);
    }
    @PostMapping("/jobs") @ResponseStatus(HttpStatus.CREATED) public Object publish(@AuthenticationPrincipal Principal p,@Valid @RequestBody JobRequests.Publish r){
        return service.publish(p,r);
    }
    @PutMapping("/jobs/{id}") public Object edit(@PathVariable long id,@AuthenticationPrincipal Principal p,@Valid @RequestBody JobRequests.Edit r){
        return service.edit(id,p,r);
    }
    @PostMapping("/jobs/{id}/increase") public Object increase(@PathVariable long id,@AuthenticationPrincipal Principal p,@Valid @RequestBody JobRequests.Increase r){
        return service.increase(id,p,r.amount());
    }
    @PostMapping("/jobs/{id}/accept") public Object accept(@PathVariable long id,@AuthenticationPrincipal Principal p,@Valid @RequestBody JobRequests.Accept r){
        return service.accept(id,p,r.expectedPrice());
    }
    @PostMapping("/jobs/{id}/cancel") public Object cancel(@PathVariable long id,@AuthenticationPrincipal Principal p,@Valid @RequestBody JobRequests.Cancel r){
        return service.cancel(id,p,r.reason());
    }
    @PostMapping("/assignments/{id}/status") public Object state(@PathVariable long id,@AuthenticationPrincipal Principal p,@Valid @RequestBody JobRequests.State r){
        return service.assignmentState(id,p,r.status());
    }
}
