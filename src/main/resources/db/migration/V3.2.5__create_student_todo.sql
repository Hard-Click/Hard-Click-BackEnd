-- 학생이 직접 추가하는 할 일('+ 할 일 추가'). 학습 스케줄 화면에서 AI 슬롯과 함께 보인다.
--
-- schedule_slot 에 넣지 않고 별도 테이블로 두는 이유(중요 - 합치지 말 것):
--   1) schedule_slot.lesson_id 가 NOT NULL 이다. 사용자가 적은 할 일엔 강의가 없다.
--   2) schedule_slot -> weekly_schedule -> enrollment 로 묶여 있어 수강 코스가 없으면 붙일 데가 없다.
--   3) 무엇보다 weekly_schedule 은 '리플로우 스냅샷'이다. 재생성될 때마다 새 버전이 활성화되므로
--      (활성 = 같은 enrollment+week_no 중 최신), 사용자가 적은 할 일을 슬롯에 넣으면
--      야간 리플로우가 돌 때 화면에서 사라진다. 사용자가 직접 쓴 데이터를 배치가 날리면 안 된다.
--   -> AI 계획(schedule_slot)은 AI 소유, 이 테이블은 학생 소유. 조회에서만 합친다.
--      "학생은 AI 계획을 편집하지 않고 완료 체크만"이라는 기존 소유 모델을 깨지 않는 방식이다.
--
-- member_id 는 cross-domain 참조 - FK 없이 컬럼+인덱스만 둔다 (ARCHITECTURE.md 패턴).
CREATE TABLE IF NOT EXISTS student_todo (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    member_id  BIGINT       NOT NULL,
    title      VARCHAR(100) NOT NULL COMMENT '할 일 제목 - 화면에 그대로 노출',
    subject    VARCHAR(30)  NULL COMMENT '과목(캘린더 색상 구분용). 미지정 가능',
    plan_date  DATE         NOT NULL COMMENT '할 일 날짜',
    start_time TIME         NULL COMMENT '시작 시각. NULL 이면 타임테이블에 배치되지 않고 목록에만 보인다',
    end_time   TIME         NULL COMMENT '종료 시각. start_time 이 있으면 필수(서비스에서 강제)',
    status     ENUM('PLANNED','DONE') NOT NULL DEFAULT 'PLANNED'
        COMMENT 'schedule_slot 과 달리 MISSED 가 없다 - 미달성 판정은 AI 계획에만 적용된다',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- 캘린더(기간 조회)/오늘 할 일 둘 다 (member_id, plan_date) 로 조회한다.
    KEY idx_todo_member_date (member_id, plan_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
