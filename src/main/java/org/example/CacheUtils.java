package org.example;


import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class CacheUtils {
    private static CacheManager cacheManager;
    public static void stopCache() {
        cacheManager.close();
    }
    public static void putInCache(Map.Entry<String, String> entry, Cache<String, String> cache) {
        cache.put(entry.getKey(),entry.getValue());
    }
    public static String getFromCache(String key, Cache<String, String> cache) {
        return cache.get(key);
    }
    public static String[] getCacheDates(String dateFrom, String dateTo, Cache<String, String> cache) {
        LocalDate startDate = LocalDate.parse(dateFrom);
        LocalDate endDate = LocalDate.parse(dateTo);
        boolean stop = false;
        do {
            String dateString = startDate.toString();
            if(cache.containsKey(dateString)) startDate = startDate.plusDays(1);
            else stop = true;
        }
        while(startDate.isBefore(endDate) && !stop);
        stop = false;
        do {
            String dateString = endDate.toString();
            if(cache.containsKey(dateString)) endDate = startDate.minusDays(1);
            else stop = true;
        }
        while(endDate.isAfter(startDate) && !stop);
        return new String[] {startDate.toString(), endDate.toString() };
    }
    public static Cache<String, String> initCache(int cacheSize) {
        cacheManager = CacheManagerBuilder
                .newCacheManagerBuilder()
                .withCache(
                        "cache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                String.class,
                                String.class,
                                ResourcePoolsBuilder.heap(cacheSize)
                        )
                ).build();
        cacheManager.init();

        return cacheManager.getCache(
                "cache", String.class, String.class
        );
    }
}
