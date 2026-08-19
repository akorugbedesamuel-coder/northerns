package project.northerntrust.app.dto;

public class LoginResponse {

    private boolean success;
    private boolean requiresOtp;
    private String message;

    public LoginResponse() {
    }

    public LoginResponse(boolean success, String message) {
        this(success, false, message);
    }

    public LoginResponse(boolean success, boolean requiresOtp, String message) {
        this.success = success;
        this.requiresOtp = requiresOtp;
        this.message = message;
    }

    // Getters and Setters
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

    public boolean isRequiresOtp() {
        return requiresOtp;
    }

    public void setRequiresOtp(boolean requiresOtp) {
        this.requiresOtp = requiresOtp;
    }
}
