# 학습 스케줄 온보딩 API (프론트 전달용)

구독권 결제 완료 → **[스케줄 입력하기]** → 초기 설정 → 불가능 시간 → 모의고사 성적.

- 인증: 전 엔드포인트 `Authorization: Bearer {accessToken}` 필수 (미첨부 시 401)
- 응답 봉투: 성공 `{ httpStatus, message, data }` / 실패 `{ errorCode, message, timestamp, path, traceId }`
- 저장 3종은 전부 **PUT(덮어쓰기)** 이다. 화면을 되돌아와 다시 저장해도 안전하고, 같은 값을 여러 번 보내도 결과가 같다.
- **단계 순서는 서버가 강제하지 않는다.** 아무 단계나 먼저 저장해도 되고, 셋이 다 차면 `onboarded: true` 가 된다.

---

## 1. GET `/api/onboarding/me` — 진행 상태 조회

결제 직후/재진입 시 **어느 단계부터 이어갈지** 판단용. 온보딩 전에도 200 이다.

```jsonc
{
  "httpStatus": 200,
  "message": "온보딩 상태를 조회했습니다.",
  "data": {
    "profileCompleted": true,      // 1단계 저장됨
    "availabilityCompleted": true, // 2단계 저장됨
    "examScoreCompleted": false,   // 3단계 저장됨
    "onboarded": false,            // 셋 다 완료 → true
    "dailyCapMin": 300,            // 서버가 가용시간에서 유도한 하루 상한(분). 2단계 전이면 null
    "restDays": 65                 // 휴식요일 비트마스크. bit0=일 … bit6=토 (65 = 일+토)
  }
}
```

---

## 2. PUT `/api/onboarding/profile` — 1단계: 학습 스케줄 초기 설정

```jsonc
{
  "targetUniversity": "서울대학교",      // 선택, ≤100자
  "targetMajor": "컴퓨터공학부",         // 선택, ≤100자
  "admissionStrategy": "UNDECIDED",     // 필수: REGULAR(정시) | EARLY(수시) | UNDECIDED(병행·미정)
  "koreanElective": "SPEECH_WRITING",   // 선택: SPEECH_WRITING(화법과 작문) | LANGUAGE_MEDIA(언어와 매체)
  "mathElective": "CALCULUS",           // 선택: CALCULUS(미적분) | GEOMETRY(기하) | STATISTICS(확률과 통계)
  "explorationTrack": "SOCIAL",         // 선택: SOCIAL(사탐) | SCIENCE(과탐) | MIXED(혼합)
  "explorationSubject1": "세계지리",     // 선택, ≤40자 (자유 문자열)
  "explorationSubject2": "세계사",       // 선택, ≤40자
  "secondLanguage": false,              // 제2외국어/한문 응시 여부
  "studyPreference": "NONE"             // 필수: MORNING(아침형) | EVENING(저녁형) | NONE(무관)
}
```

→ `200 { "message": "초기 설정이 저장되었습니다.", "data": null }`

> 화면 기본 선택값이 `admissionStrategy=UNDECIDED`, `explorationTrack=SOCIAL`, `studyPreference=NONE` 이다.
> 탐구 과목은 지금 **자유 문자열**이다(코드 enum 아님) — 드롭다운 옵션 목록은 프론트가 갖고 있고 서버는 이름만 저장한다.

---

## 3. PUT `/api/onboarding/availability` — 2단계: 불가능한 시간 체크

**체크된(=불가능한) 칸만** 보낸다. 30분 단위 그리드를 그대로 옮긴 형태다.

- `dayOfWeek`: **0=일, 1=월 … 6=토** (화면 컬럼이 월~일 순서면 매핑 주의)
- `slots`: 30분 슬롯 인덱스 **0~47**. `0`=00:00~00:30, `16`=08:00~08:30, `47`=23:30~24:00
  → `slot = 시*2 + (분>=30 ? 1 : 0)`
- 목록에 없는 요일 / 빈 `slots` = **그 요일 종일 가능**
- 종일 불가능한 요일 = 그 요일이 **쉬는 날**로 자동 계산된다 (`restDays` 비트마스크)

```jsonc
{
  "unavailable": [
    { "dayOfWeek": 1, "slots": [16, 17, 18, 19] },   // 월 08:00~10:00 불가능
    { "dayOfWeek": 0, "slots": [0, 1, 2, /* … */ 47] } // 일요일 종일 불가능 → 쉬는 날
  ]
}
```

→ `200 { "message": "불가능한 시간이 저장되었습니다.", "data": null }`

서버가 하는 일: 여집합(=가능 구간)으로 뒤집어 연속 구간 병합 → 저장, 쉬는 날 비트마스크 계산, 하루 학습 상한 유도.
저장 후 `GET /me` 의 `dailyCapMin` / `restDays` 로 결과를 확인할 수 있다.

| 에러 | 상황 |
|---|---|
| `OB001` | `dayOfWeek` 가 0~6 밖 |
| `OB002` | `slots` 에 0~47 밖 값 |
| `OB003` | **7일 전부 종일 불가능** → 일정을 만들 수 없음. 화면에서 막아주는 게 좋다 |

---

## 4. PUT `/api/onboarding/exam-scores` — 3단계: 최근 모의고사 성적

**원점수**를 보내면 서버가 등급(1~9)으로 변환해 저장한다. 프론트는 등급을 계산하지 않는다.

```jsonc
{
  "examDate": "2026-06-04",   // 선택. 미지정 시 오늘. 미래 날짜 불가
  "scores": [                 // 입력한 영역만 보내면 된다(최소 1개)
    { "subjectArea": "KOREAN",        "subjectName": "화법과 작문", "rawScore": 92 },
    { "subjectArea": "MATH",          "subjectName": "미적분",      "rawScore": 81 },
    { "subjectArea": "ENGLISH",                                     "rawScore": 88 },
    { "subjectArea": "HISTORY",                                     "rawScore": 42 },
    { "subjectArea": "EXPLORATION_1", "subjectName": "세계지리",     "rawScore": 45 },
    { "subjectArea": "EXPLORATION_2", "subjectName": "세계사",       "rawScore": 33 }
  ]
}
```

→ `200 { "message": "모의고사 성적이 저장되었습니다.", "data": null }`

**영역별 원점수 만점이 다르다 — 화면 입력 상한도 여기 맞춰야 한다:**

| `subjectArea` | 화면 라벨 | 만점 | `subjectName` |
|---|---|---:|---|
| `KOREAN` | 국어 | 100 | 필요 |
| `MATH` | 수학 | 100 | 필요 |
| `ENGLISH` | 영어 | 100 | **없음**(보내도 무시) |
| `HISTORY` | 한국사 | **50** | **없음**(보내도 무시) |
| `EXPLORATION_1` | 탐구1 | **50** | 필요 |
| `EXPLORATION_2` | 탐구2 | **50** | 필요 |

| 에러 | 상황 |
|---|---|
| `OB004` | 같은 `subjectArea` 중복 |
| `OB005` | 원점수가 해당 영역 만점 초과(예: 한국사 51) 또는 음수 |
| `OB006` | `examDate` 가 미래 |

---

## 화면 흐름 제안

```
결제완료 → [스케줄 입력하기]
   ↓  GET /api/onboarding/me         (이어하기 판단)
1단계 → PUT /api/onboarding/profile
2단계 → PUT /api/onboarding/availability
3단계 → PUT /api/onboarding/exam-scores
   ↓  onboarded: true → 스케줄 화면(GET /api/schedule/me)
```

`GET /api/schedule/me`, `/me/today`, `PATCH /api/schedule/slots/{id}/complete` 는 **별도 브랜치(미머지 패치)** 에 있다. 온보딩과 배포 시점이 다를 수 있으니 확인 필요.

---

## 프론트가 알아야 할 제약

1. **하루 학습 상한은 화면이 안 물어본다 — 물어보면 안 된다.** 서버가 가용시간에서 유도하기로 확정했다(2026-07-15).
   온보딩에 "하루 몇 분?" 입력을 추가하지 말 것. 결과값은 `GET /me` 의 `dailyCapMin` 으로만 확인한다.
   (강사가 강의등록 때 넣는 코스별 강도 상한과는 다른 값이다. 저건 코스 단위, 이건 학생 단위 총량.)
2. **목표대학·입시전략·선택과목·학습성향은 저장만 하고 아직 아무 것도 안 바꾼다.** AI 스케줄러가 읽지 않는다(수집 단계).
   화면에는 정상 노출하되, "이 값이 일정에 반영됩니다" 같은 카피는 쓰면 안 된다.
3. **성적은 원점수로 받는다(확정).** 학생에게 등급을 직접 고르게 하는 방향은 검토 후 채택하지 않았다.
4. **등급 변환값은 아직 확정 데이터가 아니다.** 영어·한국사(절대평가)는 정확하지만 국어·수학·탐구(상대평가)는
   임시 등급컷이고 실제 데이터를 별도 확보해 교체할 예정이다.
   **변환된 등급을 사용자에게 보여주는 UI 는 교체 전까지 만들면 안 된다.**
