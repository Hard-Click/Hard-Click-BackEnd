-- V3.1.7 에서 daily_cap_min 이 student_capacity(학생 단위)로 이동함에 따라
-- 코스 단위 컬럼은 제거한다.
-- ※ V3.1.2 머지 직후이고 온보딩 기능이 아직 미구현(JPA 엔티티 없음)이라 이 컬럼에
--   실데이터가 없어 백필 없이 제거한다. 만약 실데이터가 생긴 뒤라면 student_capacity 로
--   백필을 선행해야 한다.
ALTER TABLE enrollment_onboarding
    DROP COLUMN daily_cap_min;
