package com.xiamen.metro.message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 缓存服务
 * 提供多级缓存策略和热点数据管理
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheManager cacheManager;

    // 缓存命名空间
    private static final String HOT_DATA_KEY_PREFIX = "hot:data:";
    private static final String QUERY_RESULT_KEY_PREFIX = "query:result:";
    private static final String USER_SESSION_PREFIX = "user:session:";
    private static final String FILE_METADATA_PREFIX = "file:metadata:";

    /**
     * 设置热点数据缓存
     */
    public void setHotData(String key, Object value, long timeout, TimeUnit unit) {
        try {
            String cacheKey = HOT_DATA_KEY_PREFIX + key;
            redisTemplate.opsForValue().set(cacheKey, value, timeout, unit);
            log.debug("设置热点数据缓存: key={}, timeout={}", cacheKey, timeout);
        } catch (Exception e) {
            log.error("设置热点数据缓存失败: key={}", key, e);
        }
    }

    /**
     * 获取热点数据缓存
     */
    public <T> T getHotData(String key, Class<T> clazz) {
        try {
            String cacheKey = HOT_DATA_KEY_PREFIX + key;
            Object value = redisTemplate.opsForValue().get(cacheKey);
            if (value != null) {
                log.debug("命中热点数据缓存: key={}", cacheKey);
                return clazz.cast(value);
            }
            log.debug("未命中热点数据缓存: key={}", cacheKey);
            return null;
        } catch (Exception e) {
            log.error("获取热点数据缓存失败: key={}", key, e);
            return null;
        }
    }

    /**
     * 设置查询结果缓存
     */
    public void setQueryResult(String queryKey, Object result, long timeout, TimeUnit unit) {
        try {
            String cacheKey = QUERY_RESULT_KEY_PREFIX + queryKey;
            redisTemplate.opsForValue().set(cacheKey, result, timeout, unit);
            log.debug("设置查询结果缓存: key={}, timeout={}", cacheKey, timeout);
        } catch (Exception e) {
            log.error("设置查询结果缓存失败: key={}", queryKey, e);
        }
    }

    /**
     * 获取查询结果缓存
     */
    public <T> T getQueryResult(String queryKey, Class<T> clazz) {
        try {
            String cacheKey = QUERY_RESULT_KEY_PREFIX + queryKey;
            Object value = redisTemplate.opsForValue().get(cacheKey);
            if (value != null) {
                log.debug("命中查询结果缓存: key={}", cacheKey);
                return clazz.cast(value);
            }
            log.debug("未命中查询结果缓存: key={}", cacheKey);
            return null;
        } catch (Exception e) {
            log.error("获取查询结果缓存失败: key={}", queryKey, e);
            return null;
        }
    }

    /**
     * 批量设置缓存
     */
    public void batchSet(Map<String, Object> dataMap, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().multiSet(dataMap);
            // 批量设置过期时间
            dataMap.keySet().forEach(key -> redisTemplate.expire(key, timeout, unit));
            log.debug("批量设置缓存成功: size={}", dataMap.size());
        } catch (Exception e) {
            log.error("批量设置缓存失败", e);
        }
    }

    /**
     * 批量获取缓存
     */
    public List<Object> batchGet(Collection<String> keys) {
        try {
            List<Object> values = redisTemplate.opsForValue().multiGet(keys);
            log.debug("批量获取缓存成功: keys.size={}, values.size={}", keys.size(), values != null ? values.size() : 0);
            return values;
        } catch (Exception e) {
            log.error("批量获取缓存失败", e);
            return null;
        }
    }

    /**
     * 设置用户会话缓存
     */
    public void setUserSession(String userId, Object sessionData) {
        try {
            String cacheKey = USER_SESSION_PREFIX + userId;
            redisTemplate.opsForValue().set(cacheKey, sessionData, 30, TimeUnit.MINUTES);
            log.debug("设置用户会话缓存: userId={}", userId);
        } catch (Exception e) {
            log.error("设置用户会话缓存失败: userId={}", userId, e);
        }
    }

    /**
     * 获取用户会话缓存
     */
    public <T> T getUserSession(String userId, Class<T> clazz) {
        try {
            String cacheKey = USER_SESSION_PREFIX + userId;
            Object value = redisTemplate.opsForValue().get(cacheKey);
            if (value != null) {
                log.debug("命中用户会话缓存: userId={}", userId);
                return clazz.cast(value);
            }
            return null;
        } catch (Exception e) {
            log.error("获取用户会话缓存失败: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 设置文件元数据缓存
     */
    public void setFileMetadata(String fileId, Object metadata) {
        try {
            String cacheKey = FILE_METADATA_PREFIX + fileId;
            redisTemplate.opsForValue().set(cacheKey, metadata, 2, TimeUnit.HOURS);
            log.debug("设置文件元数据缓存: fileId={}", fileId);
        } catch (Exception e) {
            log.error("设置文件元数据缓存失败: fileId={}", fileId, e);
        }
    }

    /**
     * 获取文件元数据缓存
     */
    public <T> T getFileMetadata(String fileId, Class<T> clazz) {
        try {
            String cacheKey = FILE_METADATA_PREFIX + fileId;
            Object value = redisTemplate.opsForValue().get(cacheKey);
            if (value != null) {
                log.debug("命中文件元数据缓存: fileId={}", fileId);
                return clazz.cast(value);
            }
            return null;
        } catch (Exception e) {
            log.error("获取文件元数据缓存失败: fileId={}", fileId, e);
            return null;
        }
    }

    /**
     * 删除缓存
     */
    public void evict(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("删除缓存: key={}", key);
        } catch (Exception e) {
            log.error("删除缓存失败: key={}", key, e);
        }
    }

    /**
     * 批量删除缓存
     */
    public void batchEvict(Collection<String> keys) {
        try {
            redisTemplate.delete(keys);
            log.debug("批量删除缓存: size={}", keys.size());
        } catch (Exception e) {
            log.error("批量删除缓存失败", e);
        }
    }

    /**
     * 清除指定模式的缓存
     */
    public void evictByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("清除模式缓存: pattern={}, size={}", pattern, keys.size());
            }
        } catch (Exception e) {
            log.error("清除模式缓存失败: pattern={}", pattern, e);
        }
    }

    /**
     * 检查缓存是否存在
     */
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("检查缓存存在性失败: key={}", key, e);
            return false;
        }
    }

    /**
     * 设置缓存过期时间
     */
    public void expire(String key, long timeout, TimeUnit unit) {
        try {
            redisTemplate.expire(key, timeout, unit);
            log.debug("设置缓存过期时间: key={}, timeout={}", key, timeout);
        } catch (Exception e) {
            log.error("设置缓存过期时间失败: key={}", key, e);
        }
    }

    /**
     * 获取缓存剩余过期时间
     */
    public long getExpire(String key) {
        try {
            Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return expire != null ? expire : -1;
        } catch (Exception e) {
            log.error("获取缓存过期时间失败: key={}", key, e);
            return -1;
        }
    }

    /**
     * 使用Spring Cache缓存数据
     */
    public void cacheWithSpringCache(String cacheName, String key, Object value) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.put(key, value);
                log.debug("使用Spring Cache缓存数据: cacheName={}, key={}", cacheName, key);
            }
        } catch (Exception e) {
            log.error("使用Spring Cache缓存数据失败: cacheName={}, key={}", cacheName, key, e);
        }
    }

    /**
     * 从Spring Cache获取缓存数据
     */
    public <T> T getFromSpringCache(String cacheName, String key, Class<T> clazz) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(key);
                if (wrapper != null) {
                    Object value = wrapper.get();
                    if (value != null) {
                        log.debug("从Spring Cache获取缓存数据: cacheName={}, key={}", cacheName, key);
                        return clazz.cast(value);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("从Spring Cache获取缓存数据失败: cacheName={}, key={}", cacheName, key, e);
            return null;
        }
    }
}