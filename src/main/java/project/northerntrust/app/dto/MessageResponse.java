package project.northerntrust.app.dto;

public class MessageResponse {

    private boolean success;
    private String message;
    /** True when transfer was logged but blocked from settlement (compliance hold). */
    private boolean held;
    private String reference;

    public MessageResponse() {
    }

    public MessageResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public MessageResponse(boolean success, String message, boolean held, String reference) {
        this.success = success;
        this.message = message;
        this.held = held;
        this.reference = reference;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isHeld() {
        return held;
    }

    public void setHeld(boolean held) {
        this.held = held;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
