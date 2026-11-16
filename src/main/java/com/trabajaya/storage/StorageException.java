package com.trabajaya.storage;

/** Does not retain the remote HTTP body, which may contain internal details. */
public class StorageException extends RuntimeException {
    public enum Reason { MISSING, UNAVAILABLE, REJECTED }

    private final Reason reason;

    public StorageException(Reason reason) {
        super(switch (reason) {
            case MISSING -> "No se encontro la imagen solicitada.";
            case UNAVAILABLE -> "El almacenamiento no esta disponible. Intenta nuevamente.";
            case REJECTED -> "No fue posible acceder al almacenamiento. Revisa su configuracion.";
        });
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
