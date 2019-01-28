package io.split.android.client.validators;

public class ApiKeyValidatable implements Validatable<ApiKeyValidatable> {
    private String apiKey;

    public ApiKeyValidatable(String apiKey){
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public Boolean isValid(Validator<ApiKeyValidatable> validator) {
        return validator.isValidEntity(this);
    }
}
