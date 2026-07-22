// 특정 채팅방(스터디 룸)에 여러 명이 실제로 대화하듯 지속적으로 메시지를 보내는 배경 부하.
// Grafana 대시보드/영상 촬영용 트래픽 생성이 목적이라, k6 자체 응답시간 검증보다는
// "그 시간 동안 방에 메시지가 계속 오갔다"는 사실 자체가 산출물이다.
//
// 사전 준비 (필수):
//   1. CHAT_ROOM_ID로 지정할 스터디를 하나 만들고, USERS에 적을 계정들을 전부 그 스터디에 참여시켜둘 것
//      (StompChannelInterceptor가 구독 시점에 참여자 여부를 검증하므로, 참여자가 아니면 즉시 연결이 끊긴다)
//   2. 계정들은 실제 로그인 가능한 테스트 계정이어야 함(비밀번호 포함)
//
// 실행 예:
//   k6 run -e BASE_URL=https://api.flown.site -e WS_URL=wss://api.flown.site/ws-chat \
//          -e CHAT_ROOM_ID=1 -e DURATION=10m loadtest/chat-room-load.js

import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const WS_URL = __ENV.WS_URL || 'ws://localhost:8080/ws-chat';
const CHAT_ROOM_ID = __ENV.CHAT_ROOM_ID || '34';
const DURATION = __ENV.DURATION || '5m';
// WebSocketConfig의 ALLOWED_ORIGINS 중 하나와 일치해야 핸드셰이크가 통과된다.
const ORIGIN = __ENV.ORIGIN || 'https://www.flown.site';

// 이 스터디(CHAT_ROOM_ID)에 이미 참여시켜둔 테스트 계정 목록 — 실제 계정으로 교체할 것.
const USERS = [
  { username: 'loadtest01', password: 'Loadtest1!' },
  { username: 'loadtest02', password: 'Loadtest1!' },
  { username: 'loadtest03', password: 'Loadtest1!' },
  { username: 'loadtest04', password: 'Loadtest1!' },
  { username: 'loadtest05', password: 'Loadtest1!' },
];

// 실제 대화처럼 보이게 하기 위한 샘플 문구 — 필요하면 자유롭게 교체.
const MESSAGES = [
  '오늘 진도 어디까지 나가셨나요?',
  '이 부분 좀 헷갈리는데 다들 어떻게 이해하셨어요?',
  '저도 방금 그 문제 틀렸어요 ㅠㅠ',
  '해설 보니까 이해되네요',
  '오늘 스터디 몇 시에 시작할까요?',
  '자료 공유 감사합니다!',
  '다음 주차 범위 다들 확인하셨죠?',
  '넵 저는 준비 다 했습니다',
  '조금만 쉬었다 할까요',
  '좋아요 5분만 쉬어요',
];

// VU 수는 계정 개수와 무관하게 지정 가능 — 계정이 VU보다 적으면 아래 default()에서 순환 재사용한다.
const VUS = Number(__ENV.VUS) || USERS.length;

export const options = {
  scenarios: {
    chat_room_load: {
      executor: 'constant-vus',
      vus: VUS,
      duration: DURATION,
    },
  },
};

function randomOf(list) {
  return list[Math.floor(Math.random() * list.length)];
}

function login(user) {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ username: user.username, password: user.password }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(res, { '로그인 성공': (r) => r.status === 200 });
  return res.json('data.accessToken');
}

function issueSocketTicket(accessToken) {
  const res = http.post(`${BASE_URL}/api/chat/socket-tickets`, null, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  check(res, { '소켓 티켓 발급 성공': (r) => r.status === 201 });
  return res.json('data.ticket');
}

// STOMP 프레임은 순수 텍스트 프로토콜 — k6에 STOMP 클라이언트가 없으므로 직접 조립한다.
// 프레임은 반드시 NULL 바이트(\x00)로 끝나야 한다.
function stompFrame(command, headers, body) {
  const headerLines = Object.entries(headers)
    .map(([k, v]) => `${k}:${v}`)
    .join('\n');
  return `${command}\n${headerLines}\n\n${body || ''}\x00`;
}

export default function () {
  const user = USERS[(__VU - 1) % USERS.length];
  const accessToken = login(user);
  // 티켓은 발급 즉시 30초 TTL로 시작하므로, 발급 후 곧바로 CONNECT에 사용해야 한다.
  const ticket = issueSocketTicket(accessToken);

  const res = ws.connect(WS_URL, { headers: { Origin: ORIGIN } }, function (socket) {
    let subscribed = false;

    socket.on('open', () => {
      socket.send(
        stompFrame('CONNECT', {
          'accept-version': '1.1,1.0',
          'heart-beat': '0,0',
          Authorization: `Bearer ${ticket}`,
        })
      );
    });

    socket.on('message', (msg) => {
      if (!subscribed && msg.startsWith('CONNECTED')) {
        subscribed = true;

        socket.send(
          stompFrame('SUBSCRIBE', {
            id: 'sub-0',
            destination: `/sub/chat-rooms/${CHAT_ROOM_ID}`,
          })
        );

        // 3~8초 사이 무작위 간격으로 발화 — 사람이 치는 것처럼 균일하지 않게.
        socket.setInterval(() => {
          const body = JSON.stringify({ content: randomOf(MESSAGES) });
          socket.send(
            stompFrame(
              'SEND',
              {
                destination: `/pub/chat-rooms/${CHAT_ROOM_ID}`,
                'content-type': 'application/json',
              },
              body
            )
          );
        }, 3000 + Math.random() * 5000);
      }

      if (msg.startsWith('ERROR')) {
        console.error(`STOMP ERROR (VU ${__VU}): ${msg}`);
      }
    });

    // 시나리오 duration만큼만 물고 있다가 스스로 끊는다(핸드셰이크 직후 바로 끊기지 않게).
    socket.setTimeout(() => {
      socket.close();
    }, 1000 * (Number(__ENV.DURATION_SEC) || 280));
  });

  check(res, { 'WebSocket 연결 성공(101)': (r) => r && r.status === 101 });
  sleep(1);
}
