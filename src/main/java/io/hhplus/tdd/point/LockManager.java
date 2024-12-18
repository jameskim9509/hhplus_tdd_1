package io.hhplus.tdd.point;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class LockManager {
    private final Map<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    public void lock(Long userId)
    {
        // 공정성을 부여하여 함수를 호출하는 순으로 처리
        ReentrantLock lock = lockMap.computeIfAbsent(userId.toString(), (id) -> new ReentrantLock(true));
        lock.lock();
    }

    public void unlock(Long userId) throws RuntimeException
    {
        ReentrantLock lock = lockMap.get(userId.toString());
        if (lock != null)
        {
            lock.unlock();
//            lockMap.remove(userId.toString(), lock);
        } else {
            throw new RuntimeException("cannot unlock");
        }
    }
}
