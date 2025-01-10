package io.sealos.enterprise.auth.constants;

public enum ApiVersion {
    V1("/v1"),
    V2("/v2");

    private final String path;

    ApiVersion(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public static String getDefaultVersion() {
        return V1.getPath();
    }
}