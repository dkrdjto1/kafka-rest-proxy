package proxy.infra.kafka.worker;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 컨슈머 인스턴스ID
 */
@Getter
@AllArgsConstructor
public class ConsumerInstanceId {

    private final String group;    // 컨슈머 그룹명
    private final String instance; // 컨슈머 인스턴스명

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConsumerInstanceId that = (ConsumerInstanceId) o;

        if (group != null ? !group.equals(that.group) : that.group != null) {
            return false;
        }

        if (instance != null ? !instance.equals(that.instance) : that.instance != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = group != null ? group.hashCode() : 0;
        result = 31 * result + (instance != null ? instance.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ConsumerInstanceId {group='" + group + "', instance='" + instance + "'}";
    }
}
