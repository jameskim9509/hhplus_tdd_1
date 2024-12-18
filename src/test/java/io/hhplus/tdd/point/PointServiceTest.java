package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

class PointServiceTest {

    @Mock
    UserPointTable userPointTable;

    @Mock
    PointHistoryTable pointHistoryTable;

    @InjectMocks
    PointService pointService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 포인트 충전
     */
    private void assertChargePointEqual(Long userId, Long beforePoint, Long chargePoint, Long expectedPoint) {
        // given
        doReturn(new UserPoint(userId, beforePoint, 0)).when(userPointTable).selectById(userId);
        doReturn(new UserPoint(userId, beforePoint + chargePoint, System.currentTimeMillis())).when(userPointTable).insertOrUpdate(userId, beforePoint + chargePoint);

        ArgumentCaptor<Long> userIdCaptor1 = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> leftPointAmountCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> userIdCaptor2 = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> chargedPointAmountCaptor = ArgumentCaptor.forClass(Long.class);
        ;
        ArgumentCaptor<TransactionType> transactionTypeCaptor = ArgumentCaptor.forClass(TransactionType.class);
        ArgumentCaptor<Long> updateMillisCaptor = ArgumentCaptor.forClass(Long.class);

        // when
        UserPoint resultPoint = pointService.chargePoint(userId, chargePoint);

        verify(userPointTable).insertOrUpdate(userIdCaptor1.capture(), leftPointAmountCaptor.capture());
        verify(pointHistoryTable).insert(
                userIdCaptor2.capture(), chargedPointAmountCaptor.capture(), transactionTypeCaptor.capture(), updateMillisCaptor.capture()
        );

        // then
        assertEquals(userId, userIdCaptor1.getValue());
        assertEquals(expectedPoint, leftPointAmountCaptor.getValue());

        assertEquals(userId, userIdCaptor2.getValue());
        assertEquals(chargePoint, chargedPointAmountCaptor.getValue());
        assertEquals(resultPoint.updateMillis(), updateMillisCaptor.getValue());
        assertEquals(TransactionType.CHARGE, transactionTypeCaptor.getValue());

        Mockito.clearInvocations(userPointTable);
        Mockito.clearInvocations(pointHistoryTable);
    }

    /**
     * 포인트 사용
     */
    private void assertUsePointEqual(Long userId, Long beforePoint, Long usePoint, Long expectedPoint) {
        // given
        doReturn(new UserPoint(userId, beforePoint, 0)).when(userPointTable).selectById(userId);
        doReturn(new UserPoint(userId, beforePoint - usePoint, System.currentTimeMillis())).when(userPointTable).insertOrUpdate(userId, beforePoint - usePoint);

        ArgumentCaptor<Long> userIdCaptor1 = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> leftPointAmountCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> userIdCaptor2 = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> usedPointAmountCaptor = ArgumentCaptor.forClass(Long.class);
        ;
        ArgumentCaptor<TransactionType> transactionTypeCaptor = ArgumentCaptor.forClass(TransactionType.class);
        ArgumentCaptor<Long> updateMillisCaptor = ArgumentCaptor.forClass(Long.class);

        // when
        UserPoint resultPoint = pointService.usePoint(userId, usePoint);

        verify(userPointTable).insertOrUpdate(userIdCaptor1.capture(), leftPointAmountCaptor.capture());
        verify(pointHistoryTable).insert(
                userIdCaptor2.capture(), usedPointAmountCaptor.capture(), transactionTypeCaptor.capture(), updateMillisCaptor.capture()
        );

        // then
        assertEquals(userId, userIdCaptor1.getValue());
        assertEquals(expectedPoint, leftPointAmountCaptor.getValue());

        assertEquals(userId, userIdCaptor2.getValue());
        assertEquals(usePoint, usedPointAmountCaptor.getValue());
        assertEquals(resultPoint.updateMillis(), updateMillisCaptor.getValue());
        assertEquals(TransactionType.USE, transactionTypeCaptor.getValue());

        Mockito.clearInvocations(userPointTable);
        Mockito.clearInvocations(pointHistoryTable);
    }

    @DisplayName("포인트 충전 성공")
    @Test
    void chargePointSuccess() {
        assertChargePointEqual(1L, 1000L, 1000L, 2000L);
        assertChargePointEqual(2L, 1500L, 2000L, 3500L);
    }

    @DisplayName("포인트 사용 성공")
    @Test
    void usePointSuccess() {
        assertUsePointEqual(1L, 1000L, 600L, 400L);
        assertUsePointEqual(2L, 2000L, 1500L, 500L);
    }

    /**
     * 포인트 최대 잔고 초과
     */
    @DisplayName("포인트 최대 잔고 초과")
    @Test
    void 포인트_최대_잔고_초과() {
        // given
        doReturn(new UserPoint(1L, PointService.MAX_POINT, 0)).when(userPointTable).selectById(1L);
        doReturn(new UserPoint(1L, PointService.MAX_POINT + 10000L, System.currentTimeMillis())).when(userPointTable).insertOrUpdate(1L, PointService.MAX_POINT + 10000L);

        // when, then
        assertThrows(
                RuntimeException.class,
                () -> pointService.chargePoint(1L, 10000L)
        );
    }

    /**
     * 포인트 잔고 부족
     */
    @DisplayName("포인트 잔고 부족")
    @Test
    void 포인트_잔고_부족() {
        // given
        doReturn(new UserPoint(1L, 0L, 0)).when(userPointTable).selectById(1L);
        doReturn(new UserPoint(1L, -1000L, System.currentTimeMillis())).when(userPointTable).insertOrUpdate(1L, -1000L);

        // when, then
        assertThrows(
                RuntimeException.class,
                () -> pointService.usePoint(1L, 1000L)
        );
    }
}
