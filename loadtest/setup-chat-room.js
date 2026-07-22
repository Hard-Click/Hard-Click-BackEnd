// k6 채팅 부하테스트용 사전 준비 스크립트.
// 계정 5개를 실제 회원가입 플로우(이메일 인증 포함)로 만들고, 스터디 하나를 개설해서 전부 참여시킨다.
// 이메일 인증 코드는 EmailSendAdapter의 임시 로그(logs/hard-click.log)에서 읽어온다 — 로컬 전용, 1회성 준비 스크립트.
//
// 실행: node loadtest/setup-chat-room.js
// 사전조건: 로컬 서버(localhost:8080)가 EmailSendAdapter에 임시 로그 라인이 추가된 상태로 떠 있어야 한다.

const fs = require('fs');
const path = require('path');

const BASE_URL = 'http://localhost:8080';
const LOG_FILE = path.resolve(__dirname, '..', 'logs', 'hard-click.log');
const USER_COUNT = 5;

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function postJson(pathname, body, token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(`${BASE_URL}${pathname}`, {
    method: 'POST',
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const json = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(`${pathname} 실패 (${res.status}): ${JSON.stringify(json)}`);
  }
  return json.data;
}

// EmailSendAdapter의 "[LOCAL-TEST-ONLY] 인증코드 email=... code=..." 로그 라인을 찾을 때까지 짧게 폴링한다.
async function waitForVerificationCode(email) {
  const pattern = new RegExp(`\\[LOCAL-TEST-ONLY\\] 인증코드 email=${email} code=(\\d+)`);
  for (let attempt = 0; attempt < 20; attempt++) {
    if (fs.existsSync(LOG_FILE)) {
      const content = fs.readFileSync(LOG_FILE, 'utf-8');
      const matches = [...content.matchAll(new RegExp(pattern, 'g'))];
      if (matches.length > 0) {
        return matches[matches.length - 1][1]; // 가장 최근 매치
      }
    }
    await sleep(500);
  }
  throw new Error(`인증코드를 로그에서 찾지 못함: ${email} (logs/hard-click.log 확인 필요)`);
}

async function createAccount(index) {
  const username = `loadtest0${index}`;
  const email = `loadtest0${index}@gmail.com`;
  const password = 'Loadtest1!';

  console.log(`[${username}] 이메일 인증 코드 발송 요청...`);
  await postJson('/api/auth/email/send', { email });

  const code = await waitForVerificationCode(email);
  console.log(`[${username}] 인증 코드 확인: ${code}`);

  const { emailVerificationToken: token } = await postJson('/api/auth/email/verify', { email, code });

  console.log(`[${username}] 회원가입...`);
  await postJson('/api/auth/signup', {
    username,
    email,
    password,
    name: `부하테스트${index}`,
    gender: 'MALE',
    birthDate: '2000-01-01',
    phoneNumber: `010-0000-000${index}`,
    emailVerificationToken: token,
  });

  console.log(`[${username}] 로그인...`);
  const loginData = await postJson('/api/auth/login', { username, password });

  return { username, password, accessToken: loginData.accessToken, memberId: loginData.memberId };
}

async function main() {
  const users = [];
  for (let i = 1; i <= USER_COUNT; i++) {
    users.push(await createAccount(i));
  }

  console.log('\n스터디 개설...');
  const owner = users[0];
  const studyData = await postJson(
    '/api/study',
    { title: 'k6 부하테스트용 스터디', subject: 'MATH_1', maxCount: USER_COUNT, content: '실시간 채팅 부하테스트용 스터디입니다.' },
    owner.accessToken
  );
  const { groupId, chatRoomId } = studyData;
  console.log(`스터디 개설 완료: groupId=${groupId}, chatRoomId=${chatRoomId}`);

  for (const user of users.slice(1)) {
    console.log(`[${user.username}] 스터디 참여...`);
    await postJson(`/api/study/${groupId}/join`, undefined, user.accessToken);
  }

  console.log('\n=== 완료 ===');
  console.log(`CHAT_ROOM_ID=${chatRoomId}`);
  console.log('아래 계정 정보를 loadtest/chat-room-load.js의 USERS 배열에 반영하세요:');
  console.log(JSON.stringify(users.map(({ username, password }) => ({ username, password })), null, 2));
}

main().catch((err) => {
  console.error('설정 실패:', err.message);
  process.exit(1);
});
