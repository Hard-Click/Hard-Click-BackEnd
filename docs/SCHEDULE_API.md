# 학습 스케줄 API (프론트 전달용)

학습 스케줄 화면 = 월간 캘린더 + 오늘 할 일 + 타임테이블.

- 인증: 전 엔드포인트 `Authorization: Bearer {accessToken}` 필수
- 응답 봉투: 성공 `{ httpStatus, message, data }` / 실패 `{ errorCode, message, timestamp, path, traceId }`

## 핵심 개념 — 항목은 두 종류가 섞여 내려온다

| `source` | 뭐냐 | 누가 만드나 | 학생이 할 수 있는 것 |
|---|---|---|---|
| `LESSON` | AI가 배치한 강의 슬롯 | AI 스케줄러 | **완료 체크만** |
| `TODO` | 학생이 '+ 할 일 추가'로 넣은 것 | 학생 | 추가/수정/삭제/완료 |

**조회는 서버가 이미 합쳐서 준다.** 프론트가 두 번 호출해 병합할 필요 없다. 진행률(`doneCount`/`totalCount`)도 둘을 합쳐 센다.

`source` 로 완료 처리 엔드포인트를 고른다:
- `LESSON` → `PATCH /api/schedule/slots/{itemId}/complete`
- `TODO` → `PATCH /api/schedule/todos/{itemId}/complete`

> AI 슬롯은 삭제·수정이 불가능하다(엔드포인트 자체가 없다). AI 계획은 AI 소유다.

---

## 1. GET `/api/schedule/me?from=&to=` — 캘린더(월간)

`from`/`to` 미지정 시 이번 달 1일~말일.

```jsonc
{
  "httpStatus": 200,
  "data": [
    {
      "source": "LESSON",
      "itemId": 9012,
      "planDate": "2026-07-15",
      "startTime": "07:00",        // null 가능 → 타임테이블에 안 그림
      "endTime": "08:00",          // 타임테이블 블록 끝. startTime 이 null 이면 null
      "subject": "영어",            // 캘린더 색상 구분용
      "title": "Unit 3 듣기",       // 화면 노출 제목
      "plannedMinutes": 60,
      "status": "PLANNED",         // LESSON: PLANNED|DONE|MISSED
      "enrollmentId": 1063,
      "courseId": 500,
      "courseTitle": "수능 영어 실전",
      "lessonId": 301,
      "lessonTitle": "Unit 3 듣기"
    },
    {
      "source": "TODO",
      "itemId": 1,
      "planDate": "2026-07-15",
      "startTime": "20:00",
      "endTime": "21:00",
      "subject": "복습",
      "title": "지난주 복습 퀴즈",
      "plannedMinutes": 60,        // 20:00~21:00 에서 서버가 계산
      "status": "PLANNED",         // TODO: PLANNED|DONE (MISSED 없음)
      "enrollmentId": null,        // TODO 는 강의 관련 필드가 전부 null
      "courseId": null, "courseTitle": null, "lessonId": null, "lessonTitle": null
    }
  ]
}
```

**정렬**: 날짜 → 시작 시각 → itemId. **시작 시각 없는 항목은 맨 뒤.**

## 2. GET `/api/schedule/me/today` — 오늘 할 일 + 진행률

```jsonc
{ "data": { "items": [ /* 위와 같은 구조 */ ], "doneCount": 1, "totalCount": 3 } }
```
화면의 `1/3` 이 이 값이다. AI 강의와 학생 할 일을 **합쳐서** 센다.

## 3. PATCH `/api/schedule/slots/{slotId}/complete` — AI 강의 완료

→ `200`. 남의 슬롯이면 `404 SC001`.

---

## 4. POST `/api/schedule/todos` — 할 일 추가

```jsonc
{
  "title": "지난주 복습 퀴즈",   // 필수, ≤100자
  "subject": "복습",            // 선택, ≤30자. 캘린더 색상용. 없으면 색상 미지정
  "planDate": "2026-07-15",     // 필수
  "startTime": "20:00",         // 선택 (HH:mm)
  "endTime": "21:00"            // 선택 (HH:mm)
}
```
→ `201 { "data": 1 }` ← 생성된 할 일 ID

**시간 규칙 (중요):**
- 시작·종료는 **둘 다 넣거나 둘 다 빼야 한다.** 하나만 넣으면 `400 SC003`
- 종료는 시작보다 **뒤**여야 한다. 같거나 앞서면 `400 SC004`
- **둘 다 빼면 허용** → 타임테이블에는 안 뜨고 오늘 할 일 목록에만 보인다

## 5. PUT `/api/schedule/todos/{todoId}` — 할 일 수정
바디는 POST 와 동일. **완료 상태는 안 바뀐다.** → `200` / 없거나 타인 소유면 `404 SC002`

## 6. DELETE `/api/schedule/todos/{todoId}` — 할 일 삭제
→ `200` / `404 SC002`

## 7. PATCH `/api/schedule/todos/{todoId}/complete` — 할 일 완료
→ `200` / `404 SC002`

---

## 에러 코드

| 코드 | HTTP | 상황 |
|---|---|---|
| `SC001` | 404 | 본인 슬롯이 아님 |
| `SC002` | 404 | 본인 할 일이 아니거나 없음 |
| `SC003` | 400 | 시작/종료 중 하나만 입력 |
| `SC004` | 400 | 종료 ≤ 시작 |

---

## 프론트가 알아야 할 것

1. **`startTime` 이 null 인 항목이 있다.** AI가 시각을 안 붙인 슬롯 / 시간 없이 적은 할 일.
   타임테이블에 그리지 말고 목록에만 노출한다. `endTime` 도 같이 null 이다.
2. **`endTime` 은 서버가 준다.** `startTime + plannedMinutes` 를 프론트가 다시 계산하지 말 것 —
   자정을 넘기는 경우 서버가 `23:59:59` 로 잘라서 준다(그냥 더하면 블록이 뒤집힌다).
3. **`itemId` 는 source 안에서만 유일하다.** LESSON 9012 와 TODO 9012 가 동시에 존재할 수 있으므로
   리스트 key 는 `source + itemId` 로 잡아야 한다.
4. **AI 슬롯엔 수정/삭제 UI 를 붙이면 안 된다.** 엔드포인트가 없다. `source === 'TODO'` 일 때만 노출.
5. `subject` 는 자유 문자열이다(enum 아님). 색상 매핑 테이블에 없는 값이 올 수 있으니 기본색 폴백을 두자.
