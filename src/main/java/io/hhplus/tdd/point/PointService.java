package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {
    public static final Long MAX_POINT = 100_000L;
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public UserPoint getPoint(Long userId)
    {
        return userPointTable.selectById(userId);
    }

    public synchronized UserPoint chargePoint(Long userId, Long pointAmount) throws RuntimeException
    {
        UserPoint beforePoint = userPointTable.selectById(userId);
        UserPoint afterPoint =
                userPointTable.insertOrUpdate(userId, beforePoint.point() + pointAmount);

        pointHistoryTable.insert(userId, pointAmount, TransactionType.CHARGE, afterPoint.updateMillis());

        return afterPoint;
    }

    public synchronized UserPoint usePoint(Long userId, Long pointAmount) throws RuntimeException
    {
        UserPoint beforePoint = userPointTable.selectById(userId);
        UserPoint afterPoint = userPointTable.insertOrUpdate(userId, beforePoint.point() - pointAmount);

        pointHistoryTable.insert(userId, pointAmount, TransactionType.USE, afterPoint.updateMillis());

        return afterPoint;
    }

    public List<PointHistory> getPointHistory(Long userId)
    {
        return pointHistoryTable.selectAllByUserId(userId);
    }
}
