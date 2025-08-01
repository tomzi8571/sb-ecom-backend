package com.ecommerce.project.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    String resouceName;
    String field;
    String fieldName;
    Long fieldId;

    public ResourceNotFoundException() {
    }

    public ResourceNotFoundException(String resouceName, String field, String fieldName) {
        super(String.format("%s not found with %s: %s", resouceName, field, fieldName));
        this.resouceName = resouceName;
        this.field = field;
        this.fieldName = fieldName;
    }

    public ResourceNotFoundException(String resouceName, String field, Long fieldId) {
        super(String.format("%s not found with %s: %d", resouceName, field, fieldId));
        this.field = field;
        this.resouceName = resouceName;
        this.fieldId = fieldId;
    }
}
