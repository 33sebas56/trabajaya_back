package com.trabajaya.storage;
import com.trabajaya.shared.*;
import com.trabajaya.jobs.*;
import static com.trabajaya.shared.Db.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;
@Service
public class MediaService {
    private final ObjectStorage storage;
    private final Db db;
    private final JobService jobs;
    private final JobRepository repo;
    public MediaService(ObjectStorage storage,Db db,JobService jobs,JobRepository repo){
        this.storage=storage;
        this.db=db;
        this.jobs=jobs;
        this.repo=repo;
    }
    @Transactional public Object upload(Principal p,MultipartFile file){
        if(file.isEmpty()||file.getSize()>5*1024*1024)throw ApiException.bad("Selecciona una imagen PNG o JPEG de hasta 5 MB.");
        byte[] sanitized;
        try(var stream=ImageIO.createImageInputStream(file.getInputStream())){
            var readers=ImageIO.getImageReaders(stream);
            if(!readers.hasNext())throw ApiException.bad("El archivo no es una imagen valida.");
            var reader=readers.next();
            try{
                reader.setInput(stream,true,true);
                String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                if(!Set.of("png","jpeg","jpg").contains(format))throw ApiException.bad("Solo se permiten PNG y JPEG.");
                int w=reader.getWidth(0),h=reader.getHeight(0);
                if(w>4096||h>4096||(long)w*h>16000000)throw ApiException.bad("La imagen no debe superar 4096 px ni 16 megapixeles.");
                var image=reader.read(0);
                var out=new ByteArrayOutputStream();
                ImageIO.write(image,"png",out);
                sanitized=out.toByteArray();
                if(sanitized.length>8*1024*1024)throw ApiException.bad("La imagen procesada es demasiado grande. Reduce su resolucion.");
            }
            finally{
                reader.dispose();
            }
        }
        catch(IOException e){
            throw ApiException.bad("No se pudo procesar la imagen.");
        }
        String id=UUID.randomUUID().toString(),path=p.id()+"/"+id+".png";
        storage.write(path,sanitized,"image/png");
        try{
            db.update("INSERT INTO media(id,owner_id,object_path,provider,content_type,size_bytes) VALUES(:id,:owner,:path,:provider,'image/png',:size)",p("id",id,"owner",p.id(),"path",path,"provider",storage.provider(),"size",sanitized.length));
        }
        catch(RuntimeException e){
            try{
                storage.delete(path);
            }
            catch(Exception ignored){
            }
            throw e;
        }
        return Map.of("id",id,"url","/api/media/"+id,"contentType","image/png","size",sanitized.length);
    }
    public byte[] read(String id,Principal p){
        var m=db.require("SELECT * FROM media WHERE id=:id",p("id",id));
        boolean allowed=id(m,"owner_id")==p.id()||p.is("ADMIN");
        if(!allowed&&m.get("job_id")!=null){
            jobs.visible(repo.get(id(m,"job_id")),p);
            allowed=true;
        }
        if(!allowed&&m.get("assignment_id")!=null)allowed=repo.participant(repo.assignment(id(m,"assignment_id")),p);
        if(!allowed)allowed=db.count("SELECT COUNT(*) FROM users WHERE avatar_id=:id AND active=TRUE",p("id",id))>0;
        if(!allowed)throw ApiException.forbidden();
        if(!text(m,"provider").equals(storage.provider()))throw ApiException.conflict("Esta imagen pertenece a otro proveedor. Conserva el proveedor anterior o migra sus archivos antes de cambiarlo.");
        return storage.read(text(m,"object_path"));
    }
    @Transactional public void attachChat(String id,long assignment,Principal p){
        var m=db.require("SELECT * FROM media WHERE id=:id FOR UPDATE",p("id",id));
        if(id(m,"owner_id")!=p.id()||m.get("job_id")!=null||m.get("assignment_id")!=null||db.count("SELECT COUNT(*) FROM users WHERE avatar_id=:id",p("id",id))>0)throw ApiException.forbidden();
        db.update("UPDATE media SET assignment_id=:assignment WHERE id=:id",p("assignment",assignment,"id",id));
    }
}
