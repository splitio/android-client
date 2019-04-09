package io.split.android.client.utils;

import org.yaml.snakeyaml.Yaml;

import java.util.List;

public class YamlParser {

    Yaml yaml;

    public YamlParser() {
        yaml = new Yaml();
    }

    public <T> T parse(String yamlContent) {
        return (T) yaml.load(yamlContent);
    }
}
