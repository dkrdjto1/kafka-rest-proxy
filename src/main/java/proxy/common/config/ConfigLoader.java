package io.spitha.felice.common.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * classpath 외부에 있는 설정파일을 읽어오는 클래스
 */
@Component
public class ConfigLoader implements EnvironmentPostProcessor {

    // yml parser, 재사용을 목적으로 클래스 멤버 변수로 사용
    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    public static final String CONFIG_PATH = "./config/";
    public static final String KAFKA_REST_PROPERTIES = "kafka-rest.yml";

    /**
     * {@code kafka-rest.properties} 파일을 읽어 환경변수에 저장
     * @param environment
     * @param application
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        // 설정값 조회
        HashMap<String, Object> configs = this.getConfigs(KAFKA_REST_PROPERTIES);

        // 설정값이 있는 경우
        if (configs.size() > 0) {
            // 환경변수에 설정값 저장
            environment.getPropertySources().addLast(new OriginTrackedMapPropertySource(KAFKA_REST_PROPERTIES, configs));
            System.out.println("KafkaRestConfig values: " + configs);
        }
    }

    /**
     * config file load
     * @param filename // 외부 설정파일명
     * @return
     */
    @SuppressWarnings({"unchecked"})
    private HashMap<String, Object> getConfigs(String filename) {
        // 설정파일 경로
        Resource filePath = new FileSystemResource(CONFIG_PATH + filename);

        // 설정파일 내용
        Map<String, Object> configs = new HashMap<>();

        // 설정파일이 존재하는 경우
        if (filePath.exists()) {
            // 리소스가 존재하는 경우
            if (this.loadYaml(filename, filePath).size() > 0) {

                // 하나의 설정파일에 대한 리소스 조회
                configs = (Map<String, Object>) this.loadYaml(filename, filePath).get(0).getSource();
            }
        }

        // Map -> HashMap
        HashMap<String, Object> result = new HashMap<>(configs);
        
        return result;
    }
    
    /**
     * yaml 파일을 파싱하여 {@link PropertySource} 리스트 반환
     * 
     * @param filename // 외부 설정파일명
     * @param filePath // 외부 설정파일 경로
     * @return
     */
    private List<PropertySource<?>> loadYaml(String filename, Resource filePath) {
        // 설정파일이 존재하지 않는 경우
        if (!filePath.exists()) {
            throw new IllegalArgumentException("Resource " + filePath + " does not exist");
        }

        try {
            // 설정파일 리소스 반환
            return this.loader.load(filename, filePath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load yaml configuration from " + filePath, e);
        }
    }
}

