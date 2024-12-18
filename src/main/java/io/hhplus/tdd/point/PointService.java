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
    private final LockManager lockManager;

    public UserPoint getPoint(Long userId)
    {
        return userPointTable.selectById(userId);
    }

    public UserPoint chargePoint(Long userId, Long pointAmount) throws RuntimeException
    {
        lockManager.lock(userId);
        try {
            UserPoint beforePoint = userPointTable.selectById(userId);
            if (MAX_POINT < beforePoint.point() + pointAmount)
                throw new RuntimeException("최대 잔고 초과");

            UserPoint afterPoint =
                    userPointTable.insertOrUpdate(userId, beforePoint.point() + pointAmount);

            pointHistoryTable.insert(userId, pointAmount, TransactionType.CHARGE, afterPoint.updateMillis());

            return afterPoint;
        }
        finally {
            lockManager.unlock(userId);
        }
    }

    public UserPoint usePoint(Long userId, Long pointAmount) throws RuntimeException
    {
        lockManager.lock(userId);
        try{
            UserPoint beforePoint = userPointTable.selectById(userId);
            if (0 > beforePoint.point() - pointAmount)
                throw new RuntimeException("포인트 잔고 부족");

            UserPoint afterPoint = userPointTable.insertOrUpdate(userId, beforePoint.point() - pointAmount);

            pointHistoryTable.insert(userId, pointAmount, TransactionType.USE, afterPoint.updateMillis());

            return afterPoint;
        }
        finally {
            lockManager.unlock(userId);
        }
    }

    public List<PointHistory> getPointHistory(Long userId)
    {
        return pointHistoryTable.selectAllByUserId(userId);
    }
}
