package org.example.utils;


import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.core.spi.service.StatisticsService;
import org.ehcache.impl.internal.statistics.DefaultStatisticsService;
import org.example.DAO;
import org.example.Pair;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.StreamSupport;

public class CacheUtils {
    private static final StatisticsService STATISTICS_SERVICE = new DefaultStatisticsService();
    private static CacheManager CACHE_MANAGER;

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

    public static String[] getNoCacheDates(String dateFrom, String dateTo, String[] datesInCache,
                                           String[] datesInDb) {
        List<String> allCacheDates = Arrays.stream(datesInCache).toList();
        List<String> allDbDates = Arrays.stream(datesInDb).toList();
        Set<String> dateSet = new HashSet<>(allCacheDates);
        dateSet.addAll(allDbDates);
        LinkedList<String> cacheDbDates = new LinkedList<>(dateSet.stream().toList());
        Collections.sort(cacheDbDates);
        if (cacheDbDates.size() <= 0) return new String[]{dateFrom, dateTo};
        LocalDate firsDateCache = LocalDate.parse(cacheDbDates.get(0));
        LocalDate lastDateCache = LocalDate.parse(cacheDbDates.get(cacheDbDates.size() - 1));
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

    public static Pair<String[], String[]> getDatesInCacheDb
            (String dateFrom, String dateTo, Cache<String, String> cache, DAO dao, String datesTable) {
        List<String> newCacheDates = new ArrayList<>();
        List<String> newDbDates = new ArrayList<>();
        List<String> allCacheDates = getAllDatesFromCache(cache);
        allCacheDates.forEach(date -> {
            if (isDateInInterval(LocalDate.parse(date), LocalDate.parse(dateFrom),
                    LocalDate.parse(dateTo))) {
                newCacheDates.add(date);
            } else {
                if (dao != null && dao.contains(datesTable, date)) {
                    newDbDates.add(date);
                }
            }
        });
        return new Pair<>
                (newCacheDates.toArray(new String[]{}), newDbDates.toArray(new String[]{}));
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

    private long getCacheSize(String cacheName) {
        return STATISTICS_SERVICE.getCacheStatistics(cacheName).getTierStatistics()
                .get("OnHeap").getMappings();
    }
}