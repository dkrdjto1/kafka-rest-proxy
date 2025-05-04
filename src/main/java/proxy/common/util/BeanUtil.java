package proxy.common.util;

import org.springframework.context.ApplicationContext;

import proxy.common.context.ApplicationContextProvider;

/**
 * bean name을 사용하여 객체를 사용하기 위한 유틸리티 클래스
 */
public class BeanUtil {
    /**
     * 입력된 bean을 찾아 반환
     * @param bean
     * @return
     */
    public static Object getBean(String bean) {
        ApplicationContext applicationContext = ApplicationContextProvider.getApplicationContext();
        return applicationContext.getBean(bean);
    }
}
