package proxy.common.cache;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;

import com.github.benmanes.caffeine.cache.Caffeine;

import proxy.common.util.BeanUtil;

@EnableCaching
@Configuration
public class ProxyCacheManager {
    /**
     * 캐시 설정
     * @return {@link CacheManager}
     */
    @Bean(name = CacheConfig.KAFKA_REST_CACHE_CONFIG_BEAN_NAME)
    CacheManager init() {
        List<CaffeineCache> cacheList = Arrays.stream(CacheConfig.values())
                .map(e -> {
                    var caffeine = Caffeine.newBuilder()
                            .expireAfterWrite(Duration.ofMinutes(e.getMinutesForExpireAfterAccess()))
                            .maximumSize(e.getMaximumSize())
                            .build();
                    return new CaffeineCache(e.name(), caffeine);
                })
                .collect(Collectors.toList());

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(cacheList);

        return cacheManager;
    }

    /**
     * 요청한 캐시 정보 삭제
     * @param cacheName
     */
    public void clear(String cacheName) {
        var cache = this.getCache(cacheName);
        if (cache != null) {
            this.getCache(cacheName).clear();
        }
    }

    /**
     * 모든 캐시 정보 삭제
     */
    public void clearAll() {
        this.getCacheManager().getCacheNames().forEach(this::clear);
    }

    /**
     * 캐시에 저장된 모든 키 반환
     * @param cacheName
     * @return {@link Set}
     */
    public Set<@NonNull Object> getKeys(CacheConfig cacheProp) {
        var cache = (CaffeineCache) this.getCache(cacheProp);
        if (cache != null) {
            var nativeCache = cache.getNativeCache();
            if (nativeCache != null) {
                return nativeCache.asMap().keySet();
            }
        }
        
        return null;
    }

    /**
     * 캐시에 저장된 데이터 건수 반환
     * @param cacheName
     * @return int
     */
    public int getSize(CacheConfig cacheProp) {
        var keySet = this.getKeys(cacheProp);
        if (keySet == null || keySet.size() == 0) return 0;
        else return keySet.size();
    }

    /**
     * 캐시에 값이 있는 경우 찾아서 반환
     * @param cache
     * @param key
     * @param type
     * @return <T>
     */
    public <T> T get(CacheConfig cacheProp, Object key, Class<T> type) {
        var cache = this.getCache(cacheProp);
        if (cache != null) {
            var cachedData = cache.get(key, type);
            return cachedData;
        } else {
            return null;
        }
    }

    /**
     * 캐시에 값 저장
     * @param cacheName
     * @param key
     * @param value
     */
    public void put(CacheConfig cacheProp, Object key, Object value) {
        var cache = this.getCache(cacheProp);
        if (cache != null && value != null) {
            cache.put(key, value);
        }
    }

    /**
     * 캐시에 값 저장 (null 포함)
     * @param cacheName
     * @param key
     * @param value
     */
    public void putContainNull(CacheConfig cacheProp, String key, Object value) {
        var cache = this.getCache(cacheProp);
        if (cache != null) {
            cache.put(key, value);
        }
    }

    /**
     * 캐시에 요청 키에 대한 값이 존재하는 경우 삭제
     * @param cacheProp cache
     * @param key
     */
    public void evict(CacheConfig cacheProp, String key) {
        var cache = this.getCache(cacheProp);
        if (cache != null) {
            cache.evictIfPresent(key);
        }
    }

    /**
     * CacheManager bean object 반환
     * @return {@link CacheManager}
     */
    private CacheManager getCacheManager() {
        return (CacheManager) this.getBean(CacheConfig.KAFKA_REST_CACHE_CONFIG_BEAN_NAME);
    }

    /**
     * 요청한 캐시 정보 반환
     * @param cacheName
     * @return {@link Cache}
     */
    private Cache getCache(String cacheName) {
        return this.getCacheManager().getCache(cacheName);
    }

    /**
     * 요청한 캐시 정보 반환
     * @param cache
     * @return {@link Cache}
     */
    private Cache getCache(CacheConfig cache) {
        var cache = this.getCacheManager().getCache(cache.name());
        return cache;
    }

    /**
     * bean object NPE 방지
     * @param beanName
     * @return
     */
    private Object getBean(String beanName) {
        var beanObject = BeanUtil.getBean(beanName);
        while (ObjectUtils.isEmpty(beanObject)) {
            this.waitForBeanInitializing();
            beanObject = BeanUtil.getBean(beanName);
        }

        return beanObject;
    }

    /**
     * sleep thread
     */
    private void waitForBeanInitializing() {
        try {
            Thread.sleep(1000l);
        } catch (InterruptedException ignore) {
            // ignore
            Thread.currentThread().interrupt();
        }
    }
}
