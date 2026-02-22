package client.model;

import client.protobuf.P;

public class ProcessingResult {
    private final HandleResult result;
    private final String reason;
    private final P.Message message;

    public HandleResult getResult() {
        return result;
    }

    public P.Message getMessage() {
        return message;
    }


    public ProcessingResult(HandleResult result) {
        this.result = result;
        this.reason = null;
        this.message = null;
    }

    public ProcessingResult(HandleResult result, String reason, P.Message message) {
        this.result = result;
        this.reason = reason;
        this.message = message;
    }

    @Override
    public String toString() {
        if (message == null && reason == null) {
            return result.toString();
        } else {
            return String.format("%s - %s\n%s", result, reason, message);
        }
    }
}
