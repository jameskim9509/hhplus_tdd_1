package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

public class PontServiceIntegrationTest {

    UserPointTable userPointTable;
    PointHistoryTable pointHistoryTable;
    LockManager lockManager;

    PointService pointService;

    @BeforeEach
    void setup()
    {
        userPointTable = new UserPointTable();
        pointHistoryTable = new PointHistoryTable();
        lockManager = new LockManager();

        pointService = new PointService(userPointTable, pointHistoryTable, lockManager);
    }

    /**
     * 동시 충전 시 충돌 해결
     */
    @DisplayName("동시 충전시 충돌 해결")
    @Test
    void 동시_충전시_충돌_해결() throws InterruptedException {
        // given
        userPointTable.insertOrUpdate(1L, 1000L);

        int threadCount = 10;
        ExecutorService excecutorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++)
        {
            excecutorService.submit(() -> {
                try{
                    pointService.chargePoint(1L, 1L);
                }
                finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        excecutorService.shutdown();

        UserPoint resultPoint = pointService.getPoint(1L);

        // then
        assertEquals(1010L, resultPoint.point());
    }

    /**
     * 동시 사용 시 충돌 해결
     */
    @DisplayName("동시 사용시 충돌 해결")
    @Test
    void 동시_사용시_충돌_해결() throws InterruptedException {
        // given
        userPointTable.insertOrUpdate(1L, 1000L);

        int threadCount = 10;
        ExecutorService excecutorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++)
        {
            excecutorService.submit(() -> {
                try{
                    pointService.usePoint(1L, 100L);
                }
                finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        excecutorService.shutdown();

        // then
        UserPoint resultPoint = pointService.getPoint(1L);

        assertEquals(0L, resultPoint.point());
    }
}
