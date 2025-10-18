import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import ConcurrentHotelBookingWithAsyncNotifications.User;

public class RateLimiter {


    private final ConcurrentHashMap<String, UserWindowData> userDataMap;
    private final int maxRequests;
    private final long windowSizeInMillis;

    public RateLimiter(int maxRequests, long windowSizeInMillis) {
        this.userDataMap = new ConcurrentHashMap<>();
        this.maxRequests = maxRequests;
        this.windowSizeInMillis = windowSizeInMillis;
    }

    public boolean allowRequest(String userId){
        UserWindowData userData = userDataMap.computeIfAbsent(userId, k -> new UserWindowData());
        return userData.tryAcquire();
    }

    public int getCurrentCount(String userId){
        UserWindowData userData = userDataMap.get(userId);
        if(userData == null)return 0;
        return userData.getCurrentCount();
    }

    private class UserWindowData {
        private final ReentrantLock lock = new ReentrantLock();

        private long prevWindowStart;
        private int prevWindowCount;

        private long currWindowStart;
        private int currWindowCount;

        public UserWindowData() {
            long now = Instant.now().toEpochMilli();
            this.currWindowStart = now;
            this.prevWindowStart = now - windowSizeInMillis;
            this.currWindowCount = 0;
            this.prevWindowCount = 0;
        }

        public boolean tryAcquire(){
            lock.lock();
            try {
                long now = Instant.now().toEpochMilli();
                slideWindow(now);

                double weightedCount = getWeightedCount(now);

                if (weightedCount < maxRequests) {
                    currWindowCount++;
                    return true;
                }

                return false;
            } finally {
                lock.unlock();
            }
        }

        private double getWeightedCount(long now) {
            long timeSinceWindowStart = now - currWindowStart;
            double overlapPercent = 1.0 - ((double) timeSinceWindowStart / windowSizeInMillis);
            overlapPercent = Math.max(0,overlapPercent);
            return (prevWindowCount * overlapPercent) + currWindowCount;
        }

        private void slideWindow(long now) {
            long timeSinceWindowStart = now - currWindowStart;

            if(timeSinceWindowStart >= windowSizeInMillis) {
                prevWindowStart = currWindowStart;
                prevWindowCount = currWindowCount;

                currWindowStart = now;
                currWindowCount = 0;
            }
        }

        public int getCurrentCount() {
            lock.lock();
            try {
                long now = Instant.now().toEpochMilli();
                slideWindow(now);
                return (int) Math.ceil(getWeightedCount(now));
            } finally {
                lock.unlock();
            }
        }
    }
    
}
