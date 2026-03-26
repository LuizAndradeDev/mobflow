package com.mobflow.taskservice.exception;

import com.mobflow.taskservice.exception.enums.ErrorType;
import org.springframework.http.HttpStatus;

public class TaskServiceException extends RuntimeException {

    private final ErrorType errorType;
    private final HttpStatus status;

    public TaskServiceException(ErrorType errorType, HttpStatus status) {
        super(errorType.name());
        this.errorType = errorType;
        this.status = status;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static TaskServiceException boardNotFound() {
        return new TaskServiceException(ErrorType.BOARD_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    public static TaskServiceException listNotFound() {
        return new TaskServiceException(ErrorType.TASK_LIST_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    public static TaskServiceException taskNotFound() {
        return new TaskServiceException(ErrorType.TASK_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    public static TaskServiceException accessDenied() {
        return new TaskServiceException(ErrorType.ACCESS_DENIED, HttpStatus.FORBIDDEN);
    }

    public static TaskServiceException memberNotFound() {
        return new TaskServiceException(ErrorType.WORKSPACE_MEMBER_NOT_FOUND, HttpStatus.NOT_FOUND);
    }
}
