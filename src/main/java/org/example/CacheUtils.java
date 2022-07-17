package org.example;


import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.core.spi.service.StatisticsService;
import org.ehcache.core.statistics.CacheStatistics;
import org.ehcache.impl.internal.statistics.DefaultStatisticsService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.StreamSupport;

import static org.example.Main.CACHE_DATES_NAME;

public class CacheUtils {
    private static CacheManager CACHE_MANAGER;
    private static final StatisticsService STATISTICS_SERVICE = new DefaultStatisticsService();

    public static void stopCache() {
        CACHE_MANAGER.close();
    }

    public static Cache<String, String> initCache(int cacheSize, String cacheName) {
        CACHE_MANAGER = CacheManagerBuilder
                .newCacheManagerBuilder()
                .using(STATISTICS_SERVICE)
                .withCache(
                        cacheName,
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                String.class,
                                String.class,
                                ResourcePoolsBuilder.heap(cacheSize)
                        )
                ).build();
        CACHE_MANAGER.init();
        return CACHE_MANAGER.getCache(
                cacheName, String.class, String.class
        );
    }

    public static String[] getNoCacheDates(String dateFrom, String dateTo, Cache<String, String> cache) {
        CacheStatistics ehCacheStat = STATISTICS_SERVICE.getCacheStatistics(CACHE_DATES_NAME);
        long cacheSize = ehCacheStat.getTierStatistics().get("OnHeap").getMappings();
        if (cacheSize == 0) return new String[]{dateFrom, dateTo};

        List<String> allCacheDates = getAllDatesFromCache(cache);
        Collections.sort(allCacheDates);
        LocalDate firsDateCache = LocalDate.parse(allCacheDates.get(0));
        LocalDate lastDateCache = LocalDate.parse(allCacheDates.get(allCacheDates.size() - 1));
        LocalDate requestedStartDate = LocalDate.parse(dateFrom);
        LocalDate requestedEndDate = LocalDate.parse(dateTo);
        LocalDate startDateNew = requestedStartDate;
        LocalDate endDateNew = requestedEndDate;

        boolean isRequestedStartDateInCache = (requestedStartDate.isEqual(firsDateCache)
                || requestedStartDate.isAfter(firsDateCache)) &&
                (requestedStartDate.isEqual(lastDateCache) || requestedStartDate.isBefore(lastDateCache));
        boolean isRequestedEndDateInCache = (requestedEndDate.isEqual(lastDateCache)
                || requestedEndDate.isBefore(lastDateCache)) &&
                (requestedEndDate.isEqual(firsDateCache) || requestedEndDate.isAfter(firsDateCache));

        if (isRequestedStartDateInCache && isRequestedEndDateInCache) return null;
        if (isRequestedStartDateInCache) {
            startDateNew = lastDateCache.plusDays(1);
            return new String[]{startDateNew.toString(), endDateNew.toString()};
        }
        if (isRequestedEndDateInCache) {
            endDateNew = firsDateCache.minusDays(1);
            return new String[]{startDateNew.toString(), endDateNew.toString()};
        }
        return new String[]{requestedStartDate.toString(), requestedEndDate.toString()};
    }

    private static List<String> getAllDatesFromCache(Cache<String, String> cache) {
        List<Cache.Entry<String, String>> values =
                StreamSupport.stream(cache.spliterator(), false).toList();
        return new LinkedList<>(values.stream().map(Cache.Entry::getKey).toList());
    }

    public static void putMapInCache(Map<String, String> requestResponseMap, Cache<String, String> cache) {
        requestResponseMap.forEach(cache::put);
    }

    private static boolean isDateInInterval(LocalDate date, LocalDate dateStart, LocalDate dateEnd) {
        return (dateStart.isEqual(date) || dateStart.isBefore(date)) &&
                (dateEnd.isEqual(date) || dateEnd.isAfter(date));
    }

    public static String[] getDatesInCache(String dateFrom, String dateTo, Cache<String, String> cache) {
        CacheStatistics ehCacheStat = STATISTICS_SERVICE.getCacheStatistics(CACHE_DATES_NAME);
        long cacheSize = ehCacheStat.getTierStatistics().get("OnHeap").getMappings();
        if (cacheSize == 0) return new String[]{};
        List<String> newCacheDates = new ArrayList<>();
        List<String> allCacheDates = getAllDatesFromCache(cache);
        allCacheDates.forEach(date -> {
            if (isDateInInterval(LocalDate.parse(date), LocalDate.parse(dateFrom),
                    LocalDate.parse(dateTo))) {
                newCacheDates.add(date);
            }
        });
        return newCacheDates.toArray(new String[]{});
    }

    public static Map<String, String> filterCacheToMap(String[] cacheDates, Cache<String, String> cache) {
        List<String> cacheDatesList = Arrays.stream(cacheDates).toList();
        Map<String, String> map = new HashMap<>();
        cache.forEach((e -> {
            if (cacheDatesList.contains(e.getKey())) {
                map.put(e.getKey(), e.getValue());
            }
        }));
        return map;
    }
}
