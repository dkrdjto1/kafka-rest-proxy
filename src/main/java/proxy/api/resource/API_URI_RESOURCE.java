package proxy.api.resource;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * kafka rest proxy uri resources
 */
@Component
public class API_URI_RESOURCE {
    public List<String> GET = initGet();
    public List<String> POST = initPost();
    public List<String> DELETE = initDelete();

    private List<String> initGet() {
        return Arrays.asList(
            CONSUMERS_PARAM_INSTANCES_PARAM_OFFSETS,
            CONSUMERS_PARAM_INSTANCES_PARAM_SUBSCRIPTION,
            CONSUMERS_PARAM_INSTANCES_PARAM_ASSIGNMENTS,
            CONSUMERS_PARAM_INSTANCES_PARAM_RECORDS
        );
    }

    private List<String> initPost() {
        return Arrays.asList(
            TOPICS_PARAM,
            TOPICS_PARAM_PARTITIONS_PARAM,
            CONSUMERS_PARAM,
            CONSUMERS_PARAM_INSTANCES_PARAM_OFFSETS,
            CONSUMERS_PARAM_INSTANCES_PARAM_COMMITTED_OFFSETS,
            CONSUMERS_PARAM_INSTANCES_PARAM_SUBSCRIPTION,
            CONSUMERS_PARAM_INSTANCES_PARAM_ASSIGNMENTS,
            CONSUMERS_PARAM_INSTANCES_PARAM_POSITIONS,
            CONSUMERS_PARAM_INSTANCES_PARAM_POSITIONS_BEGINNING,
            CONSUMERS_PARAM_INSTANCES_PARAM_POSITIONS_END
        );
    }

    private List<String> initDelete() {
        return Arrays.asList(
            CONSUMERS_PARAM_INSTANCES_PARAM,
            CONSUMERS_PARAM_INSTANCES_PARAM_SUBSCRIPTION
        );
    }

    // Producer
    public static final String TOPICS_PARAM = "/topics/{topic_name}";
    public static final String TOPICS_PARAM_PARTITIONS_PARAM = "/topics/{topic_name}/partitions/{partition_id}";

    // Consumer
    public static final String CONSUMERS_PARAM = "/consumers/{group_name}";
    public static final String CONSUMERS_PARAM_INSTANCES_PARAM = "/consumers/{group_name}/instances/{instance}";
    public static final String CONSUMERS_PARAM_INSTANCES_PARAM_OFFSETS = "/consumers/{group_name}/instances/{instance}/offsets";
    public static final String CONSUMERS_PARAM_INSTANCES_PARAM_COMMITTED_OFFSETS = "/consumers/{group_name}/instances/{instance}/committed-offsets";
    public static final String CONSUMERS_PARAM_INSTANCES_PARAM_SUBSCRIPTION = "/consumers/{group_name}/instances/{instance}/subscription";
    public static final String CONSUMERS_PARAM_INSTANCES_PARAM_ASSIGNMENTS = "/consumers/{group_name}/instances/{instance}/assignments";
    public static final String CONSUMERS_PARAM_INSTANCES_PARAM_POSITIONS = "/consumers/{group_name}/instances/{instance}/positions";
    public static final String CONSUMERS_PARAM_INSTANCES_PARAM_POSITIONS_BEGINNING = "/consumers/{group_name}/instances/{instance}/positions/beginning";
    public static final String CONSUMERS_PARAM_INSTANCES_PARAM_POSITIONS_END = "/consumers/{group_name}/instances/{instance}/positions/end";
    public static final String CONSUMERS_PARAM_INSTANCES_PARAM_RECORDS = "/consumers/{group_name}/instances/{instance}/records";
}